package com.mail.ann.ui.compose;


import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import com.mail.ann.DI;
import com.mail.ann.FontSizes;
import com.mail.ann.helper.CrLfConverter;
import com.mail.ann.message.html.DisplayHtml;
import com.mail.ann.ui.R;
import com.mail.ann.activity.MessageCompose;
import com.mail.ann.mailstore.AttachmentResolver;
import com.mail.ann.message.QuotedTextMode;
import com.mail.ann.message.SimpleMessageFormat;
import com.mail.ann.ui.helper.DisplayHtmlUiFactory;
import com.mail.ann.view.MessageWebView;
import com.mail.ann.view.WebViewConfigProvider;


public class QuotedMessageMvpView {
    private final DisplayHtml displayHtml = DI.get(DisplayHtmlUiFactory.class).createForMessageCompose();
    private final WebViewConfigProvider webViewConfigProvider = DI.get(WebViewConfigProvider.class);

    private final Button mQuotedTextShow;
    private final View mQuotedTextBar;
    private final ImageButton mQuotedTextEdit;
    private final EditText mQuotedText;
    private final MessageWebView mQuotedHTML;
    private final EditText mMessageContentView;
    private final ImageButton mQuotedTextDelete;


    public QuotedMessageMvpView(MessageCompose messageCompose) {
        mQuotedTextShow = messageCompose.findViewById(R.id.quoted_text_show);
        mQuotedTextBar = messageCompose.findViewById(R.id.quoted_text_bar);
        mQuotedTextEdit = messageCompose.findViewById(R.id.quoted_text_edit);
        mQuotedTextDelete = messageCompose.findViewById(R.id.quoted_text_delete);
        mQuotedText = messageCompose.findViewById(R.id.quoted_text);
        mQuotedText.getInputExtras(true).putBoolean("allowEmoji", true);

        mQuotedHTML = messageCompose.findViewById(R.id.quoted_html);
        mQuotedHTML.configure(webViewConfigProvider.createForMessageCompose());
        // Disable the ability to click links in the quoted HTML page. I think this is a nice feature, but if someone
        // feels this should be a preference (or should go away all together), I'm ok with that too. -achen 20101130
        mQuotedHTML.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return true;
            }
        });

        mMessageContentView = messageCompose.findViewById(R.id.message_content);
    }

    public void setOnClickPresenter(final QuotedMessagePresenter presenter) {
        OnClickListener onClickListener = new OnClickListener() {
            @Override
            public void onClick(View view) {
                int id = view.getId();
                if (id == R.id.quoted_text_show) {
                    presenter.onClickShowQuotedText();
                } else if (id == R.id.quoted_text_delete) {
                    presenter.onClickDeleteQuotedText();
                } else if (id == R.id.quoted_text_edit) {
                    presenter.onClickEditQuotedText();
                }
            }
        };

        mQuotedTextShow.setOnClickListener(onClickListener);
        mQuotedTextEdit.setOnClickListener(onClickListener);
        mQuotedTextDelete.setOnClickListener(onClickListener);
    }

    public void addTextChangedListener(TextWatcher draftNeedsChangingTextWatcher) {
        mQuotedText.addTextChangedListener(draftNeedsChangingTextWatcher);
    }

    public void showOrHideQuotedText(QuotedTextMode mode, SimpleMessageFormat quotedTextFormat) {
        switch (mode) {
            case NONE: {
                mQuotedTextShow.setVisibility(View.GONE);
                mQuotedTextBar.setVisibility(View.GONE);
                mQuotedText.setVisibility(View.GONE);
                mQuotedHTML.setVisibility(View.GONE);
                mQuotedTextEdit.setVisibility(View.GONE);
                break;
            }
            case HIDE: {
                mQuotedTextShow.setVisibility(View.VISIBLE);
                mQuotedTextBar.setVisibility(View.GONE);
                mQuotedText.setVisibility(View.GONE);
                mQuotedHTML.setVisibility(View.GONE);
                mQuotedTextEdit.setVisibility(View.GONE);
                break;
            }
            case SHOW: {
                mQuotedTextShow.setVisibility(View.GONE);
                mQuotedTextBar.setVisibility(View.VISIBLE);

                if (quotedTextFormat == SimpleMessageFormat.HTML) {
                    mQuotedText.setVisibility(View.GONE);
                    mQuotedHTML.setVisibility(View.VISIBLE);
                    mQuotedTextEdit.setVisibility(View.VISIBLE);
                } else {
                    mQuotedText.setVisibility(View.VISIBLE);
                    mQuotedHTML.setVisibility(View.GONE);
                    mQuotedTextEdit.setVisibility(View.GONE);
                }
                break;
            }
        }
    }

    public void setFontSizes(FontSizes mFontSizes, int fontSize) {
        mFontSizes.setViewTextSize(mQuotedText, fontSize);
    }

    public void setQuotedHtml(String quotedContent, AttachmentResolver attachmentResolver) {
        mQuotedHTML.displayHtmlContentWithInlineAttachments(
                displayHtml.wrapMessageContent(quotedContent),
                attachmentResolver, null);
    }

    public void setQuotedText(String quotedText) {
        mQuotedText.setText(CrLfConverter.toLf(quotedText));
    }

    // TODO we shouldn't have to retrieve the state from the view here
    public String getQuotedText() {
        return CrLfConverter.toCrLf(mQuotedText.getText());
    }

    public void setMessageContentCharacters(String text) {
        mMessageContentView.setText(CrLfConverter.toLf(text));
    }

    public void setMessageContentCursorPosition(int messageContentCursorPosition) {
        mMessageContentView.setSelection(messageContentCursorPosition);
    }
}
