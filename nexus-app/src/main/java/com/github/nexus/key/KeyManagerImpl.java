package com.github.nexus.key;

import com.github.nexus.config.Config;
import com.github.nexus.config.KeyData;
import com.github.nexus.config.KeyDataConfig;
import com.github.nexus.config.PrivateKeyType;
import com.github.nexus.key.exception.KeyNotFoundException;
import com.github.nexus.config.keys.KeyConfig;
import com.github.nexus.config.keys.KeyEncryptor;
import com.github.nexus.nacl.Key;
import com.github.nexus.nacl.KeyPair;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class KeyManagerImpl implements KeyManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeyManagerImpl.class);

    /**
     * A list of all pub/priv keys that are attached to this node
     */
    private final Set<KeyPair> ourKeys;

    private final KeyPair defaultKeys;

    private final KeyEncryptor keyEncryptor;

    private KeyManagerImpl(final KeyEncryptor keyEncryptor, final List<KeyData> keys) {
        this.keyEncryptor = Objects.requireNonNull(keyEncryptor);

        this.ourKeys = new HashSet<>();
        keys.forEach(this::loadKeypair);

        this.defaultKeys = ourKeys.iterator().next();

    }

    public KeyManagerImpl(final KeyEncryptor keyEncryptor, final Config configuration) {
        this(keyEncryptor, configuration.getKeys());
    }

    @Override
    public Key getPublicKeyForPrivateKey(final Key privateKey) {
        LOGGER.debug("Attempting to find public key for the private key {}", privateKey);

        final Key publicKey = ourKeys
            .stream()
            .filter(keypair -> Objects.equals(keypair.getPrivateKey(), privateKey))
            .findFirst()
            .map(KeyPair::getPublicKey)
            .orElseThrow(
                () -> new KeyNotFoundException("Private key " + privateKey + " not found when searching for public key")
            );

        LOGGER.debug("Found public key {} for private key {}", publicKey, privateKey);

        return publicKey;
    }

    @Override
    public Key getPrivateKeyForPublicKey(final Key publicKey) {
        LOGGER.debug("Attempting to find private key for the public key {}", publicKey);

        final Key privateKey = ourKeys
            .stream()
            .filter(keypair -> Objects.equals(keypair.getPublicKey(), publicKey))
            .findFirst()
            .map(KeyPair::getPrivateKey)
            .orElseThrow(
                () -> new KeyNotFoundException("Public key " + publicKey + " not found when searching for private key")
            );

        LOGGER.debug("Found private key {} for public key {}", privateKey, publicKey);

        return privateKey;
    }

    @Override
    public KeyPair loadKeypair(final KeyData data) {

        LOGGER.info("Attempting to load the public key {}", data.getPublicKey());
        LOGGER.info("Attempting to load the private key {}", data.getPrivateKey());

        final Key publicKey = new Key(
            Base64.getDecoder().decode(data.getPublicKey())
        );

        final Key privateKey;
        if (Objects.nonNull(data.getPrivateKey())) {
            privateKey = new Key(data.getPrivateKey().getBytes(StandardCharsets.UTF_8));
        } else if (Objects.nonNull(data.getConfig())) {
            //TODO: Evaluate whether all of this shoud have already been done in the config module
            privateKey = loadPrivateKey(data.getConfig());
        } else {
            throw new IllegalStateException("Private Key has no value or configuration");
        }

        final KeyPair keyPair = new KeyPair(publicKey, privateKey);

        ourKeys.add(keyPair);

        return keyPair;

    }

    @Override
    public Set<Key> getPublicKeys() {
        return ourKeys
            .stream()
            .map(KeyPair::getPublicKey)
            .collect(Collectors.toSet());
    }

    @Override
    public Key defaultPublicKey() {
        return defaultKeys.getPublicKey();
    }

    private Key loadPrivateKey(final KeyDataConfig privateKey) {

        LOGGER.debug("Loading the private key at path {}", privateKey);

        if (privateKey.getType() == PrivateKeyType.UNLOCKED) {
            final String keyBase64 = privateKey.getValue();
            final byte[] key = Base64.getDecoder().decode(keyBase64);
            LOGGER.debug("Private key {} loaded from path {} loaded", keyBase64, privateKey);
            return new Key(key);
        } else {

            KeyConfig keyConfig = KeyConfig.Builder.create()
                    .asalt(toBytes(privateKey.getAsalt()))
                    .password(privateKey.getPassword())
                    .value(privateKey.getValue())
                    .snonce(toBytes(privateKey.getSnonce()))
                    .argonAlgorithm(privateKey.getArgonOptions().getAlgorithm())
                    .argonIterations(privateKey.getArgonOptions().getIterations())
                    .argonMemory(privateKey.getArgonOptions().getMemory())
                    .argonParallelism(privateKey.getArgonOptions().getParallelism())
                    .build();

            return keyEncryptor.decryptPrivateKey(keyConfig);
        }

    }

    static byte[] toBytes(String str) {
        return Optional.ofNullable(str).map(String::getBytes).orElse(null);
    }

}
