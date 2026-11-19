package com.mycompany.poe.part3;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the Part 3 features, using the test data from the PoE.
 *
 * Test data messages 1 to 5 are created here and then sent, stored or
 * disregarded through the same methods the console app uses, so the arrays are
 * populated by the program and never hard-coded. Message 4 is the developer
 * entry: its recipient has no international code, so it could not be typed
 * into the app, but it can be created directly.
 */
class MessageStoreTest {

    private static final String SENDER = "Test User";

    private static final String TEXT_1 = "Did you get the cake?";
    private static final String TEXT_2 = "Where are you? You are late! I have asked you to be on time.";
    private static final String TEXT_3 = "Yohoooo, I am at your gate.";
    private static final String TEXT_4 = "It is dinner time!";
    private static final String TEXT_5 = "Ok, I am leaving without you.";

    @TempDir
    Path tempDir;

    private Message message1;
    private Message message2;
    private Message message3;
    private Message message4;
    private Message message5;

    @BeforeEach
    void setUp() {
        MessageStore.clearAll();
        MessageStore.setStorageFile(tempDir.resolve("messages.json"));

        message1 = new Message("1000000001", 0, SENDER, "+27834557896", TEXT_1);
        message2 = new Message("2000000002", 1, SENDER, "+27838884567", TEXT_2);
        message3 = new Message("3000000003", 2, SENDER, "+27834484567", TEXT_3);
        message4 = new Message("4000000004", 3, SENDER, "0838884567", TEXT_4);
        message5 = new Message("5000000005", 4, SENDER, "+27838884567", TEXT_5);
    }

    /** Test data messages 1 to 4: sent, stored, disregarded, sent. */
    private void populateMessages1To4() {
        message1.sentMessage(Message.SEND_OPTION);
        message2.sentMessage(Message.STORE_OPTION);
        message3.sentMessage(Message.DISREGARD_OPTION);
        message4.sentMessage(Message.SEND_OPTION);
    }

    /** Test data messages 1 to 5: message 5 is also stored. */
    private void populateMessages1To5() {
        populateMessages1To4();
        message5.sentMessage(Message.STORE_OPTION);
    }

    private static List<String> textsOf(List<Message> messages) {
        List<String> texts = new ArrayList<>();

        for (Message message : messages) {
            texts.add(message.getMessageText());
        }

        return texts;
    }

    // -----------------------------------------------------------------
    // Arrays correctly populated
    // -----------------------------------------------------------------

    @Test
    @DisplayName("Sent messages array is correctly populated")
    void sentMessagesArrayPopulated() {
        populateMessages1To4();

        assertEquals(List.of("Did you get the cake?", "It is dinner time!"),
                textsOf(MessageStore.getSentMessages()));
        assertEquals(2, MessageStore.returnTotalMessages());
    }

    @Test
    @DisplayName("Disregarded messages array is correctly populated")
    void disregardedMessagesArrayPopulated() {
        populateMessages1To4();

        assertEquals(List.of("Yohoooo, I am at your gate."),
                textsOf(MessageStore.getDisregardedMessages()));
    }

    @Test
    @DisplayName("Stored messages array is correctly populated")
    void storedMessagesArrayPopulated() {
        populateMessages1To5();

        assertEquals(List.of(TEXT_2, TEXT_5), textsOf(MessageStore.getStoredMessages()));
    }

    @Test
    @DisplayName("Message hash and message ID arrays are correctly populated")
    void hashAndIdArraysPopulated() {
        populateMessages1To4();

        assertEquals(List.of("1000000001", "2000000002", "3000000003", "4000000004"),
                MessageStore.getMessageIDs());
        assertEquals(List.of("10:0:DIDCAKE", "20:1:WHERETIME", "30:2:YOHOOOOGATE",
                "40:3:ITTIME"), MessageStore.getMessageHashes());
    }

    // -----------------------------------------------------------------
    // Longest message
    // -----------------------------------------------------------------

    @Test
    @DisplayName("Display the longest message")
    void longestMessage() {
        populateMessages1To4();

        assertEquals("Where are you? You are late! I have asked you to be on time.",
                MessageStore.getLongestStoredMessage());
    }

    @Test
    @DisplayName("Longest message when nothing is stored")
    void longestMessageWhenNoneStored() {
        assertEquals("There are no stored messages.", MessageStore.getLongestStoredMessage());
    }

    // -----------------------------------------------------------------
    // Search for a message ID
    // -----------------------------------------------------------------

    @Test
    @DisplayName("Search for a message ID (test data message 4)")
    void searchByMessageId() {
        populateMessages1To4();

        Message found = MessageStore.searchByMessageId("4000000004");

        assertEquals("0838884567", found.getRecipient());
        assertEquals("It is dinner time!", found.getMessageText());
    }

    @Test
    @DisplayName("Search for a message ID that does not exist")
    void searchByMessageIdNotFound() {
        populateMessages1To4();

        assertNull(MessageStore.searchByMessageId("9999999999"));
    }

    // -----------------------------------------------------------------
    // Search for all messages for a recipient
    // -----------------------------------------------------------------

    @Test
    @DisplayName("Search all messages sent or stored for a recipient")
    void searchByRecipient() {
        populateMessages1To5();

        assertEquals(List.of(TEXT_2, TEXT_5),
                textsOf(MessageStore.searchByRecipient("+27838884567")));
    }

    @Test
    @DisplayName("Search for a recipient with no messages")
    void searchByRecipientNoMatches() {
        populateMessages1To5();

        assertTrue(MessageStore.searchByRecipient("+27000000000").isEmpty());
    }

    // -----------------------------------------------------------------
    // Delete a message using its hash
    // -----------------------------------------------------------------

    @Test
    @DisplayName("Delete a message using a message hash (test message 2)")
    void deleteMessageByHash() throws IOException {
        populateMessages1To4();

        String result = MessageStore.deleteMessageByHash(message2.getMessageHash());

        assertEquals("Message: \"Where are you? You are late! I have asked you to be "
                + "on time.\" successfully deleted.", result);
        assertTrue(MessageStore.getStoredMessages().isEmpty());
        assertFalse(MessageStore.getMessageIDs().contains("2000000002"));
        assertFalse(MessageStore.getMessageHashes().contains("20:1:WHERETIME"));
        assertFalse(Files.readString(tempDir.resolve("messages.json")).contains("2000000002"));
    }

    @Test
    @DisplayName("Deleting with a hash that does not exist")
    void deleteMessageByHashNotFound() {
        populateMessages1To4();

        assertEquals("No stored message found with that hash.",
                MessageStore.deleteMessageByHash("99:9:NOTHERE"));
        assertEquals(1, MessageStore.getStoredMessages().size());
    }

    // -----------------------------------------------------------------
    // Reports and sender / recipient list
    // -----------------------------------------------------------------

    @Test
    @DisplayName("Display report of the sent messages")
    void sentReport() {
        populateMessages1To4();

        String report = MessageStore.getSentReport();

        assertTrue(report.contains(message1.getMessageHash()));
        assertTrue(report.contains("+27834557896"));
        assertTrue(report.contains(TEXT_1));
        assertTrue(report.contains(message4.getMessageHash()));
        assertTrue(report.contains("0838884567"));
        assertTrue(report.contains(TEXT_4));
        assertFalse(report.contains(TEXT_3));
    }

    @Test
    @DisplayName("Display report of the stored messages")
    void storedReport() {
        populateMessages1To5();

        String report = MessageStore.getStoredReport();

        assertTrue(report.contains(message2.getMessageHash()));
        assertTrue(report.contains(message5.getMessageHash()));
        assertTrue(report.contains("Sender: Test User"));
        assertTrue(report.contains("Recipient: +27838884567"));
        assertTrue(report.contains(TEXT_2));
        assertTrue(report.contains(TEXT_5));
        assertTrue(report.endsWith("Total messages: 2"));
    }

    @Test
    @DisplayName("Display the sender and recipient of all stored messages")
    void sendersAndRecipients() {
        populateMessages1To5();

        String expected = String.join(System.lineSeparator(),
                "Sender: Test User | Recipient: +27838884567",
                "Sender: Test User | Recipient: +27838884567");

        assertEquals(expected, MessageStore.getSendersAndRecipients());
    }

    // -----------------------------------------------------------------
    // Reading the JSON file into the stored messages array
    // -----------------------------------------------------------------

    @Test
    @DisplayName("JSON file is read into the stored messages array")
    void readJsonFileIntoArray() {
        populateMessages1To5();
        MessageStore.clearAll();   // as if the program had been restarted

        assertTrue(MessageStore.loadStoredMessages());

        List<Message> loaded = MessageStore.getStoredMessages();
        assertEquals(2, loaded.size());
        assertEquals("2000000002", loaded.get(0).getMessageId());
        assertEquals(message2.getMessageHash(), loaded.get(0).getMessageHash());
        assertEquals(SENDER, loaded.get(0).getSender());
        assertEquals("+27838884567", loaded.get(0).getRecipient());
        assertEquals(TEXT_2, loaded.get(0).getMessageText());
        assertEquals(TEXT_5, loaded.get(1).getMessageText());
        assertEquals(List.of("2000000002", "5000000005"), MessageStore.getMessageIDs());
    }

    @Test
    @DisplayName("Reading a file that does not exist gives an empty array")
    void readMissingFile() {
        assertTrue(MessageStore.loadStoredMessages());
        assertTrue(MessageStore.getStoredMessages().isEmpty());
    }

    @Test
    @DisplayName("Storing after a restart keeps the earlier stored messages")
    void storeAfterRestartKeepsEarlierMessages() {
        message2.sentMessage(Message.STORE_OPTION);
        MessageStore.clearAll();   // as if the program had been restarted

        MessageStore.loadStoredMessages();
        message5.sentMessage(Message.STORE_OPTION);
        MessageStore.clearAll();

        MessageStore.loadStoredMessages();
        assertEquals(List.of(TEXT_2, TEXT_5), textsOf(MessageStore.getStoredMessages()));
    }

    @Test
    @DisplayName("A message deleted by hash stays deleted after a restart")
    void deletedMessageStaysDeleted() {
        populateMessages1To5();
        MessageStore.deleteMessageByHash(message2.getMessageHash());
        MessageStore.clearAll();

        MessageStore.loadStoredMessages();

        assertEquals(List.of(TEXT_5), textsOf(MessageStore.getStoredMessages()));
    }
}
