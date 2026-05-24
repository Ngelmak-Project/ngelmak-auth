package org.ngelmakproject.web.rest.dto;

public record DonationStats(
		float totalAmount,
		int count,
		float averageAmount,
		float lastDonationAmount) {
}
