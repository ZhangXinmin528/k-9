package com.mail.ann.mail.store.imap;


import java.io.IOException;
import java.util.Map;

import com.mail.ann.mail.MessagingException;
import com.mail.ann.mail.filter.FixedLengthInputStream;


class FetchBodyCallback implements ImapResponseCallback {
    private Map<String, ImapMessage> mMessageMap;

    FetchBodyCallback(Map<String, ImapMessage> messageMap) {
        mMessageMap = messageMap;
    }

    @Override
    public Object foundLiteral(ImapResponse response,
                               FixedLengthInputStream literal) throws MessagingException, IOException {
        if (response.getTag() == null &&
                ImapResponseParser.equalsIgnoreCase(response.get(1), "FETCH")) {
            ImapList fetchList = (ImapList)response.getKeyedValue("FETCH");
            String uid = fetchList.getKeyedString("UID");

            ImapMessage message = mMessageMap.get(uid);
            message.parse(literal);

            // Return placeholder object
            return 1;
        }
        return null;
    }
}
