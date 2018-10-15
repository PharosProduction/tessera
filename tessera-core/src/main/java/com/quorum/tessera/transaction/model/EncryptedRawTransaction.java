package com.quorum.tessera.transaction.model;

import com.quorum.tessera.enclave.model.MessageHash;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * The JPA entity that contains the raw transaction information
 */
@Entity
@Table(name = "ENCRYPTED_RAW_TRANSACTION")
public class EncryptedRawTransaction implements Serializable {

    @EmbeddedId
    @AttributeOverride(
            name = "hashBytes",
            column = @Column(name = "HASH", nullable = false, unique = true, updatable = false)
    )
    private MessageHash hash;

    @Lob
    @Column(name = "ENCRYPTED_PAYLOAD", nullable = false)
    private byte[] encryptedPayload;

    @Lob
    @Column(name = "ENCRYPTED_KEY", nullable = false)
    private byte[] encryptedKey;

    @Lob
    @Column(name = "NONCE", nullable = false)
    private byte[] nonce;

    @Column(name="TIMESTAMP", updatable = false)
    private long timestamp;

    public EncryptedRawTransaction(final MessageHash hash, final byte[] encryptedPayload, final byte[] encryptedKey, final byte[] nonce) {
        this.hash = hash;
        this.encryptedPayload = encryptedPayload;
        this.encryptedKey = encryptedKey;
        this.nonce = nonce;
    }

    public EncryptedRawTransaction() {
    }

    @PrePersist
    public void onPersist() {
        this.timestamp = System.currentTimeMillis();
    }

    public MessageHash getHash() {
        return this.hash;
    }

    public void setHash(final MessageHash hash) {
        this.hash = hash;
    }


    public byte[] getEncryptedPayload() {
        return encryptedPayload;
    }

    public void setEncryptedPayload(byte[] encryptedPayload) {
        this.encryptedPayload = encryptedPayload;
    }

    public byte[] getEncryptedKey() {
        return encryptedKey;
    }

    public void setEncryptedKey(byte[] encryptedKey) {
        this.encryptedKey = encryptedKey;
    }

    public byte[] getNonce() {
        return nonce;
    }

    public void setNonce(byte[] nonce) {
        this.nonce = nonce;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    @Override
    public int hashCode() {
        return 47 * 3 + Objects.hashCode(this.hash);
    }

    @Override
    public boolean equals(final Object obj) {

        return (obj instanceof EncryptedRawTransaction) &&
            Objects.equals(this.hash, ((EncryptedRawTransaction) obj).hash);
    }


}
