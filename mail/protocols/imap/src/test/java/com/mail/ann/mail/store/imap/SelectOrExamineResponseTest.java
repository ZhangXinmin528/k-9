package com.mail.ann.mail.store.imap;


import org.junit.Test;

import static com.mail.ann.mail.store.imap.ImapResponseHelper.createImapResponse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;


public class SelectOrExamineResponseTest {
    @Test
    public void parse_withSelectResponse_shouldReturnOpenModeReadWrite() throws Exception {
        ImapResponse imapResponse = createImapResponse("x OK [READ-WRITE] Select completed.");

        SelectOrExamineResponse result = SelectOrExamineResponse.parse(imapResponse);

        assertNotNull(result);
        assertEquals(true, result.hasOpenMode());
        assertEquals(OpenMode.READ_WRITE, result.getOpenMode());
    }

    @Test
    public void parse_withExamineResponse_shouldReturnOpenModeReadOnly() throws Exception {
        ImapResponse imapResponse = createImapResponse("x OK [READ-ONLY] Examine completed.");

        SelectOrExamineResponse result = SelectOrExamineResponse.parse(imapResponse);

        assertNotNull(result);
        assertEquals(true, result.hasOpenMode());
        assertEquals(OpenMode.READ_ONLY, result.getOpenMode());
    }

    @Test
    public void parse_withoutResponseCode_shouldReturnHasOpenModeFalse() throws Exception {
        ImapResponse imapResponse = createImapResponse("x OK Select completed.");

        SelectOrExamineResponse result = SelectOrExamineResponse.parse(imapResponse);

        assertNotNull(result);
        assertEquals(false, result.hasOpenMode());
    }

    @Test
    public void getOpenMode_withoutResponseCode_shouldThrow() throws Exception {
        ImapResponse imapResponse = createImapResponse("x OK Select completed.");
        SelectOrExamineResponse result = SelectOrExamineResponse.parse(imapResponse);
        assertNotNull(result);

        try {
            result.getOpenMode();
            fail("Expected exception");
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    public void parse_withInvalidResponseText_shouldReturnHasOpenModeFalse() throws Exception {
        ImapResponse imapResponse = createImapResponse("x OK [()] Examine completed.");

        SelectOrExamineResponse result = SelectOrExamineResponse.parse(imapResponse);

        assertNotNull(result);
        assertEquals(false, result.hasOpenMode());
    }

    @Test
    public void parse_withUnknownResponseText_shouldReturnHasOpenModeFalse() throws Exception {
        ImapResponse imapResponse = createImapResponse("x OK [FUNKY] Examine completed.");

        SelectOrExamineResponse result = SelectOrExamineResponse.parse(imapResponse);

        assertNotNull(result);
        assertEquals(false, result.hasOpenMode());
    }

    @Test
    public void parse_withUntaggedResponse_shouldReturnNull() throws Exception {
        ImapResponse imapResponse = createImapResponse("* OK [READ-WRITE] Select completed.");

        SelectOrExamineResponse result = SelectOrExamineResponse.parse(imapResponse);

        assertNull(result);
    }

    @Test
    public void parse_withoutOkResponse_shouldReturnNull() throws Exception {
        ImapResponse imapResponse = createImapResponse("x BYE");

        SelectOrExamineResponse result = SelectOrExamineResponse.parse(imapResponse);

        assertNull(result);
    }
}
