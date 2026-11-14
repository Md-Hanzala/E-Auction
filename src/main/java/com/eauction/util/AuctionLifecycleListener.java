package com.eauction.util;

import com.eauction.dao.AuctionDAO;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@WebListener
public class AuctionLifecycleListener implements ServletContextListener {

    private ScheduledExecutorService scheduler;
    private final AuctionDAO auctionDAO = new AuctionDAO();

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("[BidSphere Engine] Starting Automated Auction Lifecycle Daemon...");

        // Allocate a dedicated daemon thread for lifecycle tasks
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "BidSphere-AutoCloser");
            thread.setDaemon(true);
            return thread;
        });

        // Run closeExpiredAuctions() every 15 seconds
        scheduler.scheduleAtFixedRate(() -> {
            try {
                auctionDAO.closeExpiredAuctions();
            } catch (Exception e) {
                System.err.println("[BidSphere Engine] Error during auto-close check: " + e.getMessage());
            }
        }, 5, 15, TimeUnit.SECONDS);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("[BidSphere Engine] Shutting down Lifecycle Daemon...");
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }
}
