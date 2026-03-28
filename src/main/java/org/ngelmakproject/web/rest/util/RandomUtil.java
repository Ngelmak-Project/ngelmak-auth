package org.ngelmakproject.web.rest.util;

import java.security.SecureRandom;

/**
 * Utility class for generating secure, random alphanumeric keys.
 * <p>
 * This is typically used for workflows such as:
 * <ul>
 * <li>Account activation tokens</li>
 * <li>Password reset codes</li>
 * <li>Temporary verification keys</li>
 * </ul>
 *
 * The generated format follows the pattern: {@code XXXX-XXX-XXXX}
 * where each {@code X} is an uppercase alphanumeric character.
 */
public final class RandomUtil {

	private static final String ALPHANUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
	private static final SecureRandom RANDOM = new SecureRandom();
	private static final String DEFAULT_PATTERN = "XXXX-XXX-XXXX";

	// Prevent instantiation
	private RandomUtil() {
	}

	/**
	 * Generates a secure random key using the default pattern
	 * {@code XXXX-XXX-XXXX}.
	 *
	 * @return a randomly generated alphanumeric key
	 */
	public static String generateKey() {
		return generateFromPattern(DEFAULT_PATTERN);
	}

	/**
	 * Generates a random alphanumeric string based on a pattern.
	 * <p>
	 * Any 'X' in the pattern is replaced with a random alphanumeric character.
	 * All other characters (such as dashes) are preserved.
	 *
	 * @param pattern the pattern to follow (e.g., {@code XXXX-XXX-XXXX})
	 * @return a formatted random string
	 */
	private static String generateFromPattern(String pattern) {
		StringBuilder result = new StringBuilder(pattern.length());

		for (char c : pattern.toCharArray()) {
			if (c == 'X') {
				result.append(ALPHANUM.charAt(RANDOM.nextInt(ALPHANUM.length())));
			} else {
				result.append(c);
			}
		}

		return result.toString();
	}
}
