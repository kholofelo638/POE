package com.mycompany.poe.part3;

import java.util.Locale;
import java.util.Random;

/**
 * Represents a single QuickChat message.
 *
 * Each message has a randomly generated ten-digit ID, a message number, a
 * sender, a recipient, the message text and an auto-generated message hash.
 * When the user chooses what to do with a message (send, disregard or store),
 * the message is added to the matching array held by the MessageStore class.
 */
public class Message {

    // ---------------------------------------------------------------------
    // Options shown after a message has been completed.
    // ---------------------------------------------------------------------
    public static final int SEND_OPTION = 1;
    public static final int DISREGARD_OPTION = 2;
    public static final int STORE_OPTION = 3;

    // ---------------------------------------------------------------------
    // Message constants, so the console app and the unit tests always use
    // exactly the same wording.
    // ---------------------------------------------------------------------
    public static final String MESSAGE_READY = "Message ready to send.";

    public static final String CELL_SUCCESS =
            "Cell phone number successfully captured.";
    public static final String CELL_FAILURE =
            "Cell phone number is incorrectly formatted or does not contain an "
            + "international code. Please correct the number and try again.";

    public static final String MESSAGE_SENT = "Message successfully sent.";
    public static final String MESSAGE_DISREGARDED = "Press 0 to delete the message.";
    public static final String MESSAGE_STORED = "Message successfully stored.";
    public static final String MESSAGE_NOT_STORED =
            "The message could not be stored; please try again.";
    public static final String INVALID_OPTION =
            "Invalid option; please choose 1, 2 or 3.";

    private static final int MAX_MESSAGE_LENGTH = 250;
    private static final int MAX_ID_LENGTH = 10;

    private static final Random RANDOM = new Random();

    // Part 1's Login class already contains the tested cell number check, so it
    // is reused here instead of duplicating the regular expression.
    private static final Login CELL_CHECKER = new Login();

    // ---------------------------------------------------------------------
    // Data belonging to this message.
    // ---------------------------------------------------------------------
    private final String messageId;
    private final int messageNumber;
    private final String sender;
    private final String recipient;
    private final String messageText;
    private final String messageHash;

    /**
     * Creates a message with a newly generated, random ten-digit message ID.
     *
     * @param messageNumber the number of this message (the loop counter)
     * @param sender        the name of the logged-in user sending the message
     * @param recipient     the recipient's cell phone number
     * @param messageText   the message to send
     */
    public Message(int messageNumber, String sender, String recipient,
                   String messageText) {
        this(generateMessageID(), messageNumber, sender, recipient, messageText);
    }

    /**
     * Creates a message with a known message ID. This is used when messages
     * are read back from the JSON file and by the unit tests, so the hash can
     * be checked against fixed test data.
     */
    public Message(String messageId, int messageNumber, String sender,
                   String recipient, String messageText) {
        this.messageId = messageId;
        this.messageNumber = messageNumber;
        this.sender = sender;
        this.recipient = recipient;
        this.messageText = messageText;
        this.messageHash = createMessageHash();
    }

    /**
     * Generates a random ten-digit message ID by appending one random digit
     * at a time. Leading zeros are allowed, so the ID is kept as a String.
     */
    private static String generateMessageID() {
        StringBuilder id = new StringBuilder();

        for (int i = 0; i < MAX_ID_LENGTH; i++) {
            id.append(RANDOM.nextInt(10));
        }

        return id.toString();
    }

    /**
     * Ensures that the message ID is not more than ten characters long.
     *
     * @return true if the message ID is valid
     */
    public boolean checkMessageID() {
        return messageId != null && messageId.length() <= MAX_ID_LENGTH;
    }

    /**
     * Returns the confirmation shown once the message ID has been created.
     */
    public String getMessageIDStatus() {
        return "Message ID generated: " + messageId;
    }

    /**
     * Ensures that the message does not exceed 250 characters.
     *
     * @param text the message entered by the user
     * @return the success message, or how many characters the message is over by
     */
    public static String checkMessageLength(String text) {
        int length = (text == null) ? 0 : text.length();

        if (length <= MAX_MESSAGE_LENGTH) {
            return MESSAGE_READY;
        }

        return "Message exceeds " + MAX_MESSAGE_LENGTH + " characters by "
                + (length - MAX_MESSAGE_LENGTH) + "; please reduce the size.";
    }

    /**
     * Ensures that the recipient's cell number contains an international code
     * and is correctly formatted, by reusing the check from Part 1.
     *
     * @param recipient the cell number entered by the user
     * @return the success or failure message for the number
     */
    public static String checkRecipientCell(String recipient) {
        if (CELL_CHECKER.checkCellPhoneNumber(recipient)) {
            return CELL_SUCCESS;
        }
        return CELL_FAILURE;
    }

    /**
     * Creates the message hash: the first two digits of the message ID, a
     * colon, the message number, a colon, then the first and last words of
     * the message, all in capital letters (for example 00:0:HITHANKS).
     * Punctuation around the first and last words is ignored.
     */
    public final String createMessageHash() {
        String[] words = messageText.trim().split("\\s+");

        String firstWord = stripPunctuation(words[0]);
        String lastWord = stripPunctuation(words[words.length - 1]);
        String idPrefix = messageId.substring(0, Math.min(2, messageId.length()));

        String hash = idPrefix + ":" + messageNumber + ":" + firstWord + lastWord;

        return hash.toUpperCase(Locale.ROOT);
    }

    /** Removes any punctuation from the start and end of a word. */
    private static String stripPunctuation(String word) {
        return word.replaceAll("^[^\\p{L}\\p{N}]+|[^\\p{L}\\p{N}]+$", "");
    }

    /**
     * Carries out the option the user chose for a completed message by adding
     * the message to the matching array.
     *
     * @param option 1 to send, 2 to disregard, 3 to store for later
     * @return the message describing what happened
     */
    public String sentMessage(int option) {
        return switch (option) {
            case SEND_OPTION -> {
                MessageStore.addSent(this);
                yield MESSAGE_SENT;
            }
            case DISREGARD_OPTION -> {
                MessageStore.addDisregarded(this);
                yield MESSAGE_DISREGARDED;
            }
            case STORE_OPTION -> storeMessage() ? MESSAGE_STORED : MESSAGE_NOT_STORED;
            default -> INVALID_OPTION;
        };
    }

    /**
     * Stores this message in the stored messages array and in the JSON file so
     * it can be sent later.
     *
     * @return true if the message was stored successfully
     */
    public boolean storeMessage() {
        return MessageStore.addStored(this);
    }

    /**
     * Returns the details shown straight after a message is sent, in the
     * required order: Message ID, Message Hash, Recipient, Message.
     */
    public String getMessageDetails() {
        return String.join(System.lineSeparator(),
                "Message ID: " + messageId,
                "Message Hash: " + messageHash,
                "Recipient: " + recipient,
                "Message: " + messageText);
    }

    /**
     * Returns every detail of the message, as used in the reports.
     */
    public String getFullDetails() {
        return String.join(System.lineSeparator(),
                "Message ID: " + messageId,
                "Message Hash: " + messageHash,
                "Sender: " + sender,
                "Recipient: " + recipient,
                "Message: " + messageText);
    }

    /**
     * Returns the report of every message sent while the program has been
     * running.
     */
    public static String printMessages() {
        return MessageStore.getSentReport();
    }

    /**
     * Returns the total number of messages sent while the program has been
     * running.
     */
    public static int returnTotalMessages() {
        return MessageStore.returnTotalMessages();
    }

    // ---------------------------------------------------------------------
    // Getters.
    // ---------------------------------------------------------------------
    public String getMessageId() {
        return messageId;
    }

    public int getMessageNumber() {
        return messageNumber;
    }

    public String getSender() {
        return sender;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getMessageText() {
        return messageText;
    }

    public String getMessageHash() {
        return messageHash;
    }
}
