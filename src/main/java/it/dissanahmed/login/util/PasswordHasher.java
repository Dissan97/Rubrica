package it.dissanahmed.login.util;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;


public final class PasswordHasher {

        // Parametri di default (possono essere regolati)
        private static final int ITERATIONS = 120_000;   // costo
        private static final int KEY_LENGTH = 256;       // bit
        private static final int SALT_LEN   = 16;        // byte

        private PasswordHasher() {}

        /* Hash di una password in chiaro con sale casuale. */
        public static String hash(String plainPassword) {
                if (plainPassword == null) throw new IllegalArgumentException("plainPassword nulla");
                byte[] salt = new byte[SALT_LEN];
                new SecureRandom().nextBytes(salt);
                byte[] derived = pbkdf2(plainPassword.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
                return "pbkdf2" + "$" + ITERATIONS + "$" +
                        Base64.getEncoder().encodeToString(salt) + "$" +
                        Base64.getEncoder().encodeToString(derived);
        }

        /* Verifica di una password in chiaro contro una stringa hash salvata. */
        public static boolean verify(String plainPassword, String stored) {
                if (plainPassword == null || stored == null) return false;
                String[] parts = stored.split("\\$");
                if (parts.length != 4 || !"pbkdf2".equals(parts[0])) return false;

                int iterations = Integer.parseInt(parts[1]);
                byte[] salt = Base64.getDecoder().decode(parts[2]);
                byte[] expected = Base64.getDecoder().decode(parts[3]);

                byte[] actual = pbkdf2(plainPassword.toCharArray(), salt, iterations, expected.length * 8);
                return slowEquals(expected, actual);
        }

        // PBKDF2 core
        private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBits) {
                try {
                        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
                        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                        return skf.generateSecret(spec).getEncoded();
                } catch (Exception e) {
                        throw new IllegalStateException("Errore PBKDF2: " + e.getMessage(), e);
                }
        }

        // Confronto a tempo costante
        private static boolean slowEquals(byte[] a, byte[] b) {
                if (a == null || b == null || a.length != b.length) return false;
                int diff = 0;
                for (int i = 0; i < a.length; i++) {
                        diff |= a[i] ^ b[i];
                }
                return diff == 0;
        }
}
