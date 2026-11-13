(function ($) {
    "use strict";

    $(function () {
        var $root = $("#customerService");
        if (!$root.length) {
            return;
        }

        var $panel = $("#customerServicePanel");
        var $toggle = $("#customerServiceToggle");
        var $close = $("#customerServiceClose");
        var $form = $("#customerServiceForm");
        var $input = $("#customerServiceInput");
        var $send = $("#customerServiceSend");
        var $messages = $("#customerServiceMessages");
        var chatUrl = $root.attr("data-chat-url");
        var history = [];
        var sending = false;
        var maxHistory = 18;
        var maxContentLength = 2000;
        var maxHistoryLength = 10000;

        function setPanelOpen(open) {
            $panel.prop("hidden", !open);
            $toggle.attr("aria-expanded", open ? "true" : "false");
            $toggle.attr("aria-label", open ? "关闭 DeepSeek 客服" : "打开 DeepSeek 客服");
            if (open) {
                $input.trigger("focus");
            } else {
                $toggle.trigger("focus");
            }
        }

        function addMessage(role, content, state) {
            var $message = $("<div>").addClass("customer-service-message customer-service-message-" + role);
            var $bubble = $("<div>").addClass("customer-service-bubble").text(content);
            if (state) {
                $message.addClass("customer-service-message-" + state);
            }
            $message.append($bubble);
            $messages.append($message);
            $messages.scrollTop($messages[0].scrollHeight);
            return $message;
        }

        function setSending(value) {
            sending = value;
            $input.prop("disabled", value);
            $send.prop("disabled", value).text(value ? "发送中..." : "发送");
        }

        function trimHistory() {
            var totalLength = 0;
            $.each(history, function (_, message) {
                totalLength += message.content.length;
            });
            while (history.length > maxHistory || totalLength > maxHistoryLength) {
                totalLength -= history[0].content.length + history[1].content.length;
                history.splice(0, 2);
            }
        }

        $toggle.on("click", function () {
            setPanelOpen($panel.prop("hidden"));
        });

        $close.on("click", function () {
            setPanelOpen(false);
        });

        $input.on("keydown", function (event) {
            var originalEvent = event.originalEvent;
            if (event.key === "Enter" && !event.shiftKey && !(originalEvent && originalEvent.isComposing) && event.keyCode !== 229) {
                event.preventDefault();
                $form.trigger("submit");
            }
        });

        $panel.on("keydown", function (event) {
            if (event.key === "Escape") {
                setPanelOpen(false);
            }
        });

        $form.on("submit", function (event) {
            event.preventDefault();
            var question = $.trim($input.val());
            if (!question || sending) {
                return;
            }

            $messages.find(".customer-service-message-error").remove();
            var $userMessage = addMessage("user", question);
            var $pendingMessage = addMessage("assistant", "正在思考…", "pending");
            var requestMessages = history.concat([{ role: "user", content: question }]);
            var headers = {};
            var $csrf = $('meta[name="csrf-token"]');
            if ($csrf.length) {
                headers["X-CSRF-Token"] = $csrf.attr("content") || "";
            }

            function restoreAfterFailure(message) {
                $pendingMessage.remove();
                $userMessage.remove();
                addMessage("error", message, "error");
                $input.val(question);
            }

            $input.val("");
            setSending(true);

            $.ajax({
                url: chatUrl,
                type: "POST",
                contentType: "application/json",
                dataType: "json",
                headers: headers,
                data: JSON.stringify({ messages: requestMessages })
            }).done(function (data) {
                var answer = data && data.data && data.data.answer;
                if (!data || data.code !== 0) {
                    restoreAfterFailure(data && typeof data.msg === "string" ? data.msg : "发送失败，请稍后重试。");
                    return;
                }
                if (typeof answer !== "string") {
                    restoreAfterFailure("发送失败，请稍后重试。");
                    return;
                }
                $pendingMessage.remove();
                addMessage("assistant", answer);
                history.push({ role: "user", content: question });
                history.push({ role: "assistant", content: answer.slice(0, maxContentLength) });
                trimHistory();
            }).fail(function (xhr) {
                var message = xhr.responseJSON && typeof xhr.responseJSON.msg === "string"
                    ? xhr.responseJSON.msg
                    : "发送失败，请稍后重试。";
                restoreAfterFailure(message);
            }).always(function () {
                setSending(false);
                $input.trigger("focus");
            });
        });
    });
})(jQuery);
