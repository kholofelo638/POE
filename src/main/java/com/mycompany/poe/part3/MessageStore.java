package com.mycompany.poe.part3;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds the arrays that QuickChat fills while the program runs, and the
 * features that work on them.
 *
 * Arrays (all populated by the user's actions, never hard-coded):
 * - sentMessages        every message that was sent
 * - disregardedMessages every message that was disregarded
 * - storedMessages      every message stored for later (read from messages.json)
 * - messageHashes       the hash of every message added to the arrays above
 * - messageIDs          the ID of every message added to the arrays above
 *
 * The messageHashes and messageIDs arrays are parallel: the entry at position
 * n in each belongs to the same message. ArrayLists are used because the
 * arrays must grow as messages are added and shrink when a message is deleted.
 */
public final class MessageStore {

    public static final String NO_SENT_MESSAGES = "No messages have been sent.";
    public static final String NO_STORED_MESSAGES = "There are no stored messages.";
    public static final String HASH_NOT_FOUND = "No stored message found with that hash.";
    public static final String DELETE_FAILED =
            "The message could not be deleted from the file; please try again.";

    private static final String NEW_LINE = System.lineSeparator();

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static final List<Message> sentMessages = new ArrayList<>();
    private static final List<Message> disregardedMessages = new ArrayList<>();
    private static final List<Message> storedMessages = new ArrayList<>();
    private static final List<String> messageHashes = new ArrayList<>();
    private static final List<String> messageIDs = new ArrayList<>();

    private static Path storageFile = Paths.get("messages.json");

    private MessageStore() {
        // Only static methods; there is nothing to create.
    }

    // =====================================================================
    // Populating the arrays
    // =====================================================================

    /** Adds a message to the sent messages array. */
    public static void addSent(Message message) {
        sentMessages.add(message);
        track(message);
    }

    /** Adds a message to the disregarded messages array. */
    public static void addDisregarded(Message message) {
        disregardedMessages.add(message);
        track(message);
    }

    /**
     * Adds a message to the stored messages array and saves the array to the
     * JSON file.
     *
     * @return true if the message was stored and the file was saved
     */
    public static boolean addStored(Message message) {
        storedMessages.add(message);

        if (!saveStoredMessages()) {
            storedMessages.remove(message);
            return false;
        }

        track(message);
        return true;
    }

    /** Records the message's hash and ID in the two parallel arrays. */
    private static void track(Message message) {
        messageHashes.add(message.getMessageHash());
        messageIDs.add(message.getMessageId());
    }

    /** Removes the message's hash and ID from the two parallel arrays. */
    private static void untrack(Message message) {
        int index = messageIDs.indexOf(message.getMessageId());

        if (index >= 0) {
            messageIDs.remove(index);
            messageHashes.remove(index);
        }
    }

    // =====================================================================
    // Reading and writing the JSON file
    // =====================================================================

    /**
     * Reads messages.json into the stored messages array, replacing whatever
     * the array held. A missing file simply means nothing has been stored yet.
     *
     * Reference:
     * Mkyong, n.d. Read and Write JSON to File using Gson. [Online]
     * Available at: https://mkyong.com/java/read-and-write-json-to-file-using-gson/
     * [Accessed 18 November 2026].
     *
     * @return true if the file was read successfully (or does not exist yet)
     */
    public static boolean loadStoredMessages() {
        for (Message message : storedMessages) {
            untrack(message);
        }
        storedMessages.clear();

        if (!Files.exists(storageFile)) {
            return true;
        }

        try (Reader reader = Files.newBufferedReader(storageFile)) {
            JsonElement root = JsonParser.parseReader(reader);

            if (root.isJsonArray()) {
                for (JsonElement element : root.getAsJsonArray()) {
                    Message message = toMessage(element.getAsJsonObject());
                    storedMessages.add(message);
                    track(message);
                }
            }

            return true;
        } catch (IOException | JsonParseException | IllegalStateException e) {
            return false;
        }
    }

    /** Builds a Message from one JSON object read from the file. */
    private static Message toMessage(JsonObject json) {
        int number = json.has("messageNumber") ? json.get("messageNumber").getAsInt() : 0;

        return new Message(readText(json, "messageId"), number,
                readText(json, "sender"), readText(json, "recipient"),
                readText(json, "messageText"));
    }

    /** Reads one text field, treating a missing field as empty text. */
    private static String readText(JsonObject json, String field) {
        return json.has(field) ? json.get(field).getAsString() : "";
    }

    /**
     * Writes the whole stored messages array to the JSON file, so the file
     * always matches the array.
     *
     * Reference:
     * Baeldung, 2024. Save Data to a JSON File with Gson. [Online]
     * Available at: https://www.baeldung.com/gson-save-file
     * [Accessed 18 October 2026].
     *
     * @return true if the file was saved
     */
    private static boolean saveStoredMessages() {
        try (Writer writer = Files.newBufferedWriter(storageFile)) {
            GSON.toJson(storedMessages, writer);
            return true;
        } catch (IOException | JsonParseException e) {
            return false;
        }
    }

    // =====================================================================
    // Stored messages menu features
    // =====================================================================

    /** Returns the sender and recipient of every stored message. */
    public static String getSendersAndRecipients() {
        if (storedMessages.isEmpty()) {
            return NO_STORED_MESSAGES;
        }

        List<String> lines = new ArrayList<>();

        for (Message message : storedMessages) {
            lines.add("Sender: " + message.getSender()
                    + " | Recipient: " + message.getRecipient());
        }

        return String.join(NEW_LINE, lines);
    }

    /** Returns the text of the longest stored message. */
    public static String getLongestStoredMessage() {
        if (storedMessages.isEmpty()) {
            return NO_STORED_MESSAGES;
        }

        Message longest = storedMessages.get(0);

        for (Message message : storedMessages) {
            if (message.getMessageText().length() > longest.getMessageText().length()) {
                longest = message;
            }
        }

        return longest.getMessageText();
    }

    /**
     * Searches the sent and stored messages for a message ID.
     *
     * @return the matching message, or null if there is none
     */
    public static Message searchByMessageId(String messageId) {
        for (Message message : getKeptMessages()) {
            if (message.getMessageId().equals(messageId)) {
                return message;
            }
        }

        return null;
    }

    /**
     * Searches the sent and stored messages for every message to a recipient.
     *
     * @return the matching messages, which is empty if there are none
     */
    public static List<Message> searchByRecipient(String recipient) {
        List<Message> matches = new ArrayList<>();

        for (Message message : getKeptMessages()) {
            if (message.getRecipient().equals(recipient)) {
                matches.add(message);
            }
        }

        return matches;
    }

    /**
     * Deletes a stored message using its message hash, removing it from the
     * stored messages array, the parallel hash and ID arrays, and the JSON file.
     *
     * @return the message confirming the deletion, or why it could not be done
     */
    public static String deleteMessageByHash(String messageHash) {
        Message found = null;

        for (Message message : storedMessages) {
            if (message.getMessageHash().equalsIgnoreCase(messageHash)) {
                found = message;
                break;
            }
        }

        if (found == null) {
            return HASH_NOT_FOUND;
        }

        int index = storedMessages.indexOf(found);
        storedMessages.remove(index);

        if (!saveStoredMessages()) {
            storedMessages.add(index, found);
            return DELETE_FAILED;
        }

        untrack(found);
        return "Message: \"" + found.getMessageText() + "\" successfully deleted.";
    }

    /** Returns the report of the full details of every stored message. */
    public static String getStoredReport() {
        return buildReport("STORED MESSAGES REPORT", storedMessages, NO_STORED_MESSAGES);
    }

    /** Returns the report of the full details of every sent message. */
    public static String getSentReport() {
        return buildReport("SENT MESSAGES REPORT", sentMessages, NO_SENT_MESSAGES);
    }

    private static String buildReport(String title, List<Message> messages,
                                      String emptyText) {
        if (messages.isEmpty()) {
            return emptyText;
        }

        StringBuilder report = new StringBuilder();
        report.append("===== ").append(title).append(" =====")
                .append(NEW_LINE).append(NEW_LINE);

        for (int i = 0; i < messages.size(); i++) {
            report.append("Message ").append(i + 1).append(NEW_LINE)
                    .append(messages.get(i).getFullDetails())
                    .append(NEW_LINE).append(NEW_LINE);
        }

        report.append("Total messages: ").append(messages.size());
        return report.toString();
    }

    /** Combines the sent and stored messages, which are the ones still kept. */
    private static List<Message> getKeptMessages() {
        List<Message> kept = new ArrayList<>(sentMessages);
        kept.addAll(storedMessages);
        return kept;
    }

    // =====================================================================
    // Totals and access to the arrays
    // =====================================================================

    /** Returns the total number of messages sent. */
    public static int returnTotalMessages() {
        return sentMessages.size();
    }

    public static List<Message> getSentMessages() {
        return Collections.unmodifiableList(sentMessages);
    }

    public static List<Message> getDisregardedMessages() {
        return Collections.unmodifiableList(disregardedMessages);
    }

    public static List<Message> getStoredMessages() {
        return Collections.unmodifiableList(storedMessages);
    }

    public static List<String> getMessageHashes() {
        return Collections.unmodifiableList(messageHashes);
    }

    public static List<String> getMessageIDs() {
        return Collections.unmodifiableList(messageIDs);
    }

    // =====================================================================
    // Helpers used by the unit tests
    // =====================================================================

    /** Changes where stored messages are saved (the default is messages.json). */
    public static void setStorageFile(Path file) {
        storageFile = file;
    }

    /** Empties every array, as if the program had just been started. */
    public static void clearAll() {
        sentMessages.clear();
        disregardedMessages.clear();
        storedMessages.clear();
        messageHashes.clear();
        messageIDs.clear();
    }
}
