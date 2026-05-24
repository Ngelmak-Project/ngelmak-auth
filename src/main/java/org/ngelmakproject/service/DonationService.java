package org.ngelmakproject.service;

import org.ngelmakproject.domain.Donation;
import org.ngelmakproject.repository.DonationRepository;
import org.ngelmakproject.web.rest.dto.DonationStats;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DonationService {

    private final DonationRepository repository;

    public DonationService(DonationRepository repository) {
        this.repository = repository;
    }

    /**
     * Create a new donation.
     *
     * @param donation the donation to create
     * @return the created donation
     * @throws IllegalArgumentException if the amount is null or negative
     */
    public Donation donate(Donation donation) {
        if (donation.getAmount() == null || donation.getAmount() < 0) {
            throw new IllegalArgumentException("Amount must be a positive integer");
        }

        return repository.save(donation);
    }

    /**
     * Get donation statistics including total amount, count, average, and last donation amount.
     *
     * @return a DonationStats object containing the statistics
     */
    @Transactional(readOnly = true)
    public DonationStats getStats() {
        var all = repository.findAll();
        var total = all.stream()
                .map(Donation::getAmount)
                .reduce(0f, (subtotal, element) -> subtotal + element);

        int count = all.size();
        float average = count == 0 ? 0 : total / count;

        Donation last = all.stream()
                .max((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .orElse(null);

        DonationStats stats = new DonationStats(total, count, average, last != null ? last.getAmount() : 0);

        return stats;
    }

    /**
     * Get the 20 most recent donations.
     *
     * @return a list of the 20 most recent donations
     */
    @Transactional(readOnly = true)
    public List<Donation> getRecentDonations() {
        return repository.findTop20ByOrderByCreatedAtDesc();
    }
}