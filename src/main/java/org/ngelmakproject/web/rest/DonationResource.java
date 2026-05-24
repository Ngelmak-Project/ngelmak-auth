package org.ngelmakproject.web.rest;

import java.util.List;

import org.ngelmakproject.domain.Donation;
import org.ngelmakproject.security.AuthoritiesConstants;
import org.ngelmakproject.service.DonationService;
import org.ngelmakproject.web.rest.dto.DonationStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Donation Controller.
 * 
 * <p>
 * Base path: /api
 * 
 * - /api
 * - └── /public # Unsecured endpoints
 * - │ ├── /donation # Endpoints for making donations and viewing recent
 * donations
 * - │ │ ├── GET /stats # Get total donations and count
 * - │ │ └── GET /recent # Get recent donations (last 20)
 * - │ │
 * - │ └── /donation # Secured endpoints for admin users
 * - │ │ └── POST / # Create a new donation (admin only)
 */
@RestController
@RequestMapping("/api")
public class DonationResource {

    private static final Logger log = LoggerFactory.getLogger(DonationResource.class);

    private final DonationService donationService;

    public DonationResource(DonationService donationService) {
        this.donationService = donationService;
    }

    /**
     * {@code POST /donation} : Create a new donation (admin only).
     *
     * @param donation the donation to create
     * @return the ResponseEntity with status 200 (OK) and with body the new
     *         donation, or with status 400 (Bad Request) if the donation is
     *         invalid, or with status 500 (Internal Server Error) if the
     *         donation couldn't be created
     */
    @PostMapping("/donation")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<Donation> donate(
            @RequestBody Donation donation) {
        log.debug("REST request to save a Donation : {}", donation);
        Donation newDonation = donationService.donate(donation);
        return ResponseEntity.ok(newDonation);
    }

    /**
     * {@code GET /public/donation/stats} : Get total donations and count.
     *
     * @return the ResponseEntity with status 200 (OK) and with body the
     *         donation stats, or with status 500 (Internal Server Error) if
     *         the stats couldn't be retrieved
     */
    @GetMapping("public/donation/stats")
    public ResponseEntity<DonationStats> getStats() {
        log.debug("REST request to get donation stats");
        DonationStats stats = donationService.getStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * {@code GET /public/donation/recent} : Get recent donations (last 20).
     *
     * @return the ResponseEntity with status 200 (OK) and with body the list
     *         of recent donations, or with status 500 (Internal Server Error)
     *         if the donations couldn't be retrieved
     */
    @GetMapping("public/donation/recent")
    public ResponseEntity<List<Donation>> getRecentDonations() {
        log.debug("REST request to get recent donations");
        List<Donation> donations = donationService.getRecentDonations();
        return ResponseEntity.ok(donations);
    }
}