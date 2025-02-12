package com.mail.ann.activity.loader;

import java.io.File;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import androidx.loader.content.AsyncTaskLoader;

import com.mail.ann.helper.MimeTypeUtil;
import com.mail.ann.message.Attachment.LoadingState;
import timber.log.Timber;

import com.mail.ann.activity.misc.Attachment;
import com.mail.ann.mail.internet.MimeUtility;

/**
 * Loader to fetch metadata of an attachment.
 */
public class AttachmentInfoLoader  extends AsyncTaskLoader<Attachment> {
    private final Attachment sourceAttachment;
    private Attachment cachedResultAttachment;


    public AttachmentInfoLoader(Context context, Attachment attachment) {
        super(context);
        if (attachment.state != LoadingState.URI_ONLY) {
            throw new IllegalArgumentException("Attachment provided to metadata loader must be in URI_ONLY state");
        }

        sourceAttachment = attachment;
    }

    @Override
    protected void onStartLoading() {
        if (cachedResultAttachment != null) {
            deliverResult(cachedResultAttachment);
        }

        if (takeContentChanged() || cachedResultAttachment == null) {
            forceLoad();
        }
    }

    @Override
    public Attachment loadInBackground() {
        Uri uri = sourceAttachment.uri;
        String contentType = sourceAttachment.contentType;

        long size = -1;
        String name = null;

        ContentResolver contentResolver = getContext().getContentResolver();

        Cursor metadataCursor = contentResolver.query(
                uri,
                new String[] { OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE },
                null,
                null,
                null);

        if (metadataCursor != null) {
            try {
                if (metadataCursor.moveToFirst()) {
                    name = metadataCursor.getString(0);
                    size = metadataCursor.getInt(1);
                }
            } finally {
                metadataCursor.close();
            }
        }

        if (name == null) {
            name = uri.getLastPathSegment();
        }

        String usableContentType = contentResolver.getType(uri);
        if (usableContentType == null && contentType != null && contentType.indexOf('*') != -1) {
            usableContentType = contentType;
        }

        if (usableContentType == null) {
            usableContentType = MimeTypeUtil.getMimeTypeByExtension(name);
        }

        if (!sourceAttachment.allowMessageType && MimeUtility.isMessageType(usableContentType)) {
            usableContentType = MimeTypeUtil.DEFAULT_ATTACHMENT_MIME_TYPE;
        }

        if (size <= 0) {
            String uriString = uri.toString();
            if (uriString.startsWith("file://")) {
                File f = new File(uriString.substring("file://".length()));
                size = f.length();
            } else {
                Timber.v("Not a file: %s", uriString);
            }
        } else {
            Timber.v("old attachment.size: %d", size);
        }
        Timber.v("new attachment.size: %d", size);

        cachedResultAttachment = sourceAttachment.deriveWithMetadataLoaded(usableContentType, name, size);
        return cachedResultAttachment;
    }
}
