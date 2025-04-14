package vm.computer.components;

import org.json.JSONObject;
import vm.computer.Machine;
import vm.computer.components.base.ComponentBase;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Objects;
import java.util.zip.CRC32;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;


public class Data extends ComponentBase{

    public int tier;

    private SecureRandom secureRandom = new SecureRandom();

    public Data(Machine machine, String address, JSONObject obj) {
        super(machine, address, obj);

        this.tier = obj.optInt("tier",3);
    }

    @Override
    public void pushProxyFields(){
        super.pushProxyFields();

        machine.lua.pushJavaFunction(args -> {
            machine.lua.pushInteger(1048576);

            return 1;
        });
        machine.lua.setField(-2, "getLimit");

        machine.lua.pushJavaFunction(args -> {
            args.checkString(1);
            String data = args.toString(1);

            machine.lua.pushByteArray(Base64.getEncoder().encode(data.getBytes()));

            return 1;
        });
        machine.lua.setField(-2, "encode64");

        machine.lua.pushJavaFunction(args -> {
            args.checkString(1);
            String data = args.toString(1);

            machine.lua.pushByteArray(Base64.getDecoder().decode(data.getBytes()));

            return 1;
        });
        machine.lua.setField(-2, "decode64");

        machine.lua.pushJavaFunction(args -> {
            args.checkByteArray(1);
            byte[] data = args.toByteArray(1);

            ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
            DeflaterOutputStream deos = new DeflaterOutputStream(baos);

            try {
                deos.write(data);
                deos.finish();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            machine.lua.pushByteArray(baos.toByteArray());
            return 1;
        });
        machine.lua.setField(-2, "deflate");

        machine.lua.pushJavaFunction(args -> {
            args.checkByteArray(1);
            byte[] data = args.toByteArray(1);

            ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
            InflaterOutputStream inos = new InflaterOutputStream(baos);

            try {
                inos.write(data);
                inos.finish();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            machine.lua.pushByteArray(baos.toByteArray());
            return 1;
        });
        machine.lua.setField(-2, "inflate");

        machine.lua.pushJavaFunction(args -> {
            args.checkByteArray(1);
            byte[] data = args.toByteArray(1);

            CRC32 fileCRC32 = new CRC32();
            fileCRC32.update(data);

            machine.lua.pushString(Long.toHexString(fileCRC32.getValue()));
            return 1;
        });
        machine.lua.setField(-2, "crc32");// don't work like in game :( but work

        if (tier <= 1) {
            machine.lua.pushJavaFunction(args -> {
                args.checkByteArray(1);
                byte[] data = args.toByteArray(1);

                try {
                    MessageDigest digest = MessageDigest.getInstance("MD5");
                    digest.update(data);
                    machine.lua.pushByteArray(digest.digest());
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }

                return 1;
            });
            machine.lua.setField(-2, "md5");


            machine.lua.pushJavaFunction(args -> {
                args.checkByteArray(1);
                byte[] data = args.toByteArray(1);

                try {
                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    digest.update(data);
                    machine.lua.pushByteArray(digest.digest());
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }

                return 1;
            });
            machine.lua.setField(-2, "sha256");

            return;
        }
        // all method of tier 2 card

        machine.lua.pushJavaFunction(args -> {
            args.checkByteArray(1);
            byte[] data = args.toByteArray(1);
            if (args.isNoneOrNil(2)) {
                try {
                    MessageDigest digest = MessageDigest.getInstance("MD5");
                    digest.update(data);
                    machine.lua.pushByteArray(digest.digest());
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
            }else{
                args.checkByteArray(2);
                byte[] key = args.toByteArray(2);

                try {
                    SecretKeySpec secretKeySpec = new SecretKeySpec(key, "HmacMD5");
                    Mac mac = Mac.getInstance("HmacMD5");
                    mac.init(secretKeySpec);
                    machine.lua.pushByteArray(mac.doFinal(data));
                } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                    throw new RuntimeException(e);
                }
            }

            return 1;
        });
        machine.lua.setField(-2, "md5");

        machine.lua.pushJavaFunction(args -> {
            args.checkByteArray(1);
            byte[] data = args.toByteArray(1);
            if (args.isNoneOrNil(2)) {
                try {
                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    digest.update(data);
                    machine.lua.pushByteArray(digest.digest());
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
            } else {
                args.checkByteArray(2);
                byte[] key = args.toByteArray(2);

                try {
                    SecretKeySpec secretKeySpec = new SecretKeySpec(key, "HmacSHA256");
                    Mac mac = Mac.getInstance("HmacSHA256");
                    mac.init(secretKeySpec);
                    machine.lua.pushByteArray(mac.doFinal(data));
                } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                    throw new RuntimeException(e);
                }
            }

            return 1;
        });
        machine.lua.setField(-2, "sha256");

        machine.lua.pushJavaFunction(args -> {
            args.checkInteger(1);
            int len = args.toInteger(1);

            if (len <= 0 || len > 1024)
                throw new IllegalArgumentException("length must be in range [1..1024]");

            byte[] target = new byte[len];
            secureRandom.nextBytes(target);
            machine.lua.pushByteArray(target);

            return 1;
        });
        machine.lua.setField(-2, "random");

        machine.lua.pushJavaFunction(args -> {
            args.checkByteArray(1);
            byte[] data = args.toByteArray(1);
            args.checkByteArray(2);
            byte[] key = args.toByteArray(2);
            args.checkByteArray(3);
            byte[] iv = args.toByteArray(3);

            if (key.length != 16)
                throw new IllegalArgumentException("expected a 128-bit AES key");
            if (iv.length != 16)
                throw new IllegalArgumentException("expected a 128-bit AES IV");

            try {
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key,"AES"), new IvParameterSpec(iv));
                machine.lua.pushByteArray(cipher.doFinal(data));
            } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                     InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
                throw new RuntimeException(e);
            }
            return 1;
        });
        machine.lua.setField(-2, "encrypt");

        machine.lua.pushJavaFunction(args -> {
            args.checkByteArray(1);
            byte[] data = args.toByteArray(1);
            args.checkByteArray(2);
            byte[] key = args.toByteArray(2);
            args.checkByteArray(3);
            byte[] iv = args.toByteArray(3);

            if (key.length != 16)
                throw new IllegalArgumentException("expected a 128-bit AES key");
            if (iv.length != 16)
                throw new IllegalArgumentException("expected a 128-bit AES IV");

            try {
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key,"AES"), new IvParameterSpec(iv));
                machine.lua.pushByteArray(cipher.doFinal(data));
            } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                     InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
                throw new RuntimeException(e);
            }
            return 1;
        });
        machine.lua.setField(-2, "decrypt");

        if (tier == 2) return;

        machine.lua.pushJavaFunction(args -> {
            int bitLen = 384;
            if (!args.isNoneOrNil(1)){
                bitLen = args.toInteger(1);
            }
            if (bitLen != 256 && bitLen != 384)
                throw new IllegalArgumentException("invalid key length, must be 256 or 384");
            try {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
                kpg.initialize(bitLen, secureRandom);
                KeyPair kp = kpg.genKeyPair();

                // Pushing the public and private keys onto the Lua stack

                machine.lua.newTable();
                machine.lua.pushJavaFunction(_args -> {machine.lua.pushBoolean(true);return 1;});
                machine.lua.setField(-2, "isPublic");
                machine.lua.pushJavaFunction(_args -> {machine.lua.pushString("ec-public");return 1;});
                machine.lua.setField(-2, "keyType");
                machine.lua.pushJavaFunction(_args -> {machine.lua.pushByteArray(kp.getPublic().getEncoded());return 1;});
                machine.lua.setField(-2, "serialize");
                machine.lua.pushString("userdata");
                machine.lua.setField(-2, "type");

                machine.lua.newTable();
                machine.lua.pushJavaFunction(_args -> {machine.lua.pushBoolean(false);return 1;});
                machine.lua.setField(-2, "isPublic");
                machine.lua.pushJavaFunction(_args -> {machine.lua.pushString("ec-private");return 1;});
                machine.lua.setField(-2, "keyType");
                machine.lua.pushJavaFunction(_args -> {machine.lua.pushByteArray(kp.getPrivate().getEncoded());return 1;});
                machine.lua.setField(-2, "serialize");
                machine.lua.pushString("userdata");
                machine.lua.setField(-2, "type");

            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }

            // Return the number of pushed values (2)
            return 2;
        });
        machine.lua.setField(-2, "generateKeyPair"); // work but don't like in game

        machine.lua.pushJavaFunction(args -> {
            args.checkByteArray(1);
            byte[] data = args.toByteArray(1);
            args.checkString(2);
            String type = args.toString(2);

            machine.lua.newTable();
            machine.lua.pushJavaFunction(_args -> {machine.lua.pushBoolean(Objects.equals(type, "ec-public"));return 1;});
            machine.lua.setField(-2, "isPublic");
            machine.lua.pushJavaFunction(_args -> {machine.lua.pushString(type);return 1;});
            machine.lua.setField(-2, "keyType");
            machine.lua.pushJavaFunction(_args -> {machine.lua.pushByteArray(data);return 1;});
            machine.lua.setField(-2, "serialize");
            machine.lua.pushString("userdata");
            machine.lua.setField(-2, "type");
            return 1;
        });
        machine.lua.setField(-2, "deserializeKey");

        machine.lua.pushJavaFunction(args -> {
            try {
                machine.lua.getField(1, "serialize");
                machine.lua.call(0, 1);
                byte[] data1 = machine.lua.toByteArray(-1);
                machine.lua.pop(1);
                Key key1 = KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(data1));
                machine.lua.getField(2, "serialize");
                machine.lua.call(0, 1);
                byte[] data2 = machine.lua.toByteArray(-1);
                machine.lua.pop(1);
                Key key2 = KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(data2));

                KeyAgreement ka = KeyAgreement.getInstance("ECDH");
                ka.init(key1);
                ka.doPhase(key2,true);

                machine.lua.pushByteArray(ka.generateSecret());
            } catch (InvalidKeySpecException | NoSuchAlgorithmException | InvalidKeyException e) {
                throw new RuntimeException(e);
            }
            return 1;
        });
        machine.lua.setField(-2, "ecdh");

        machine.lua.pushJavaFunction(args -> {
            try {
                machine.lua.checkByteArray(1);
                byte[] data = machine.lua.toByteArray(1);
                machine.lua.getField(2, "serialize");
                machine.lua.call(0, 1);
                byte[] dataKey = machine.lua.toByteArray(-1);
                machine.lua.pop(1);
                Signature sign = Signature.getInstance("SHA256withECDSA");
                if (machine.lua.isNoneOrNil(3)){
                    // Sign mode
                    PrivateKey key = KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(dataKey));
                    sign.initSign(key);
                    sign.update(data);
                    machine.lua.pushByteArray(sign.sign());
                }else {
                    // Verify mode
                    machine.lua.checkByteArray(3);
                    byte[] sig = machine.lua.toByteArray(3);
                    PublicKey key = KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(dataKey));
                    sign.initVerify(key);
                    sign.update(data);
                    machine.lua.pushBoolean(sign.verify(sig));
                }
            } catch (InvalidKeySpecException | NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
                throw new RuntimeException(e);
            }
            return 1;
        });
        machine.lua.setField(-2, "ecdsa");



    }

    @Override
    public JSONObject  toJSONObject(){
        return super.toJSONObject()
                .put("tier", tier);
    }
}
