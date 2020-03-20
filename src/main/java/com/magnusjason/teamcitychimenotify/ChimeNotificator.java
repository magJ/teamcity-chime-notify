package com.magnusjason.teamcitychimenotify;

import jetbrains.buildServer.Build;
import jetbrains.buildServer.notification.NotificatorAdapter;
import jetbrains.buildServer.notification.NotificatorRegistry;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.serverSide.TriggeredBy;
import jetbrains.buildServer.serverSide.UserPropertyInfo;
import jetbrains.buildServer.users.NotificatorPropertyKey;
import jetbrains.buildServer.users.PropertyKey;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.vcs.VcsModification;
import jetbrains.buildServer.vcs.VcsRoot;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ChimeNotificator extends NotificatorAdapter {

    public enum StatusType {
        INFO("\uD83D\uDEE0️"),
        SUCCESS("✅"),
        ERROR("\uD83D\uDD25");

        final String statusEmoji;

        StatusType(String statusEmoji) {
            this.statusEmoji = statusEmoji;
        }
    }

    private static final Logger LOGGER = Logger.getLogger(ChimeNotificator.class);

    private static final String TYPE = "ChimeNotificator";

    private static final String CHIME_WEBHOOK_KEY = "chime.WebhookUrl";
    private static final String CHIME_VERBOSE_KEY = "chime.Verbose";

    private static final PropertyKey CHIME_WEBHOOK = new NotificatorPropertyKey(TYPE, CHIME_WEBHOOK_KEY);
    private static final PropertyKey CHIME_VERBOSE = new NotificatorPropertyKey(TYPE, CHIME_VERBOSE_KEY);

    private final SBuildServer sBuildServer;

    public ChimeNotificator(NotificatorRegistry notificatorRegistry, SBuildServer sBuildServer) {
        registerNotificatorAndUserProperties(notificatorRegistry);
        this.sBuildServer = sBuildServer;
    }

    @NotNull
    public String getNotificatorType() {
        return TYPE;
    }

    @NotNull
    public String getDisplayName() {
        return "Amazon Chime Notifier";
    }

    public void notifyBuildFailed(@NotNull SRunningBuild sRunningBuild, @NotNull Set<SUser> users) {
        sendNotification(sRunningBuild, users, "failed", StatusType.ERROR);
    }

    public void notifyBuildFailedToStart(@NotNull SRunningBuild sRunningBuild, @NotNull Set<SUser> users) {
        sendNotification(sRunningBuild, users, "failed to start", StatusType.ERROR);
    }

    public void notifyBuildSuccessful(@NotNull SRunningBuild sRunningBuild, @NotNull Set<SUser> users) {
        sendNotification(sRunningBuild, users, "successful", StatusType.SUCCESS);
    }

    public void notifyLabelingFailed(@NotNull Build build, @NotNull VcsRoot vcsRoot, @NotNull Throwable throwable, @NotNull Set<SUser> users) {
        sendNotification(build, users, "labelling failed", StatusType.ERROR);
    }

    public void notifyBuildFailing(@NotNull SRunningBuild sRunningBuild, @NotNull Set<SUser> users) {
        sendNotification(sRunningBuild, users, "failing", StatusType.ERROR);
    }

    public void notifyBuildProbablyHanging(@NotNull SRunningBuild sRunningBuild, @NotNull Set<SUser> users) {
        sendNotification(sRunningBuild, users, "probably hanging", StatusType.ERROR);
    }

    public void notifyBuildStarted(@NotNull SRunningBuild sRunningBuild, @NotNull Set<SUser> users) {
        sendNotification(sRunningBuild, users, "started", StatusType.INFO);
    }

    private void registerNotificatorAndUserProperties(NotificatorRegistry notificatorRegistry) {
        List<UserPropertyInfo> userPropertyInfos = getUserPropertyInfosList();
        notificatorRegistry.register(this, userPropertyInfos);
    }

    private List<UserPropertyInfo> getUserPropertyInfosList() {
        List<UserPropertyInfo> userPropertyInfos = new ArrayList<>();

        userPropertyInfos.add(new UserPropertyInfo(CHIME_WEBHOOK_KEY, "Webhook URL"));
        userPropertyInfos.add(new UserPropertyInfo(CHIME_VERBOSE_KEY, "Verbose Messages. True or false", "false"));

        return userPropertyInfos;
    }

    private void sendNotification(Build build, Set<SUser> users, String statusText, StatusType statusType) {
        for (SUser user : users) {
            String url = user.getPropertyValue(CHIME_WEBHOOK);
            String verbose = user.getPropertyValue(CHIME_VERBOSE);
            ChimeClient chimeClient = new ChimeClient(url);

            String message = formatMessage(build, statusText, statusType, Boolean.parseBoolean(verbose));

            try {
                chimeClient.sendMessage(message);
            }
            catch (Exception e) {
                LOGGER.error("Error sending notification", e);
            }
        }
    }

    private String formatMessage(Build build, String statusText, StatusType statusType, boolean verbose) {
        String buildNumber = build.getBuildNumber();
        String buildTypeId = build.getBuildTypeExternalId();
        String buildLogUrl = sBuildServer.getRootUrl() + "/viewLog.html?buildNumber=" + buildNumber + "&buildTypeId=" + buildTypeId;

        String message =
            "/md ### " + statusType.statusEmoji + " " + build.getFullName() + " #" + buildNumber + ": " + statusText + "\n  " +
                "[View build](" + buildLogUrl + ")\n  ";

        if ((statusType == StatusType.INFO || verbose)) {
            if (build instanceof SBuild) {
                TriggeredBy triggeredBy = ((SBuild) build).getTriggeredBy();
                message += "Triggered by: " + triggeredBy.getAsString() + "\n  ";
            }

            List<? extends VcsModification> changes = build.getContainingChanges();
            if (!changes.isEmpty()) {
                String changeTableRows = changes.stream()
                    .limit(5) // Lets not go too crazy
                    .map(change -> {
                        String descriptionFirstLine = change.getDescription().split("\n")[0];
                        return "| " + change.getUserName() + " | " + descriptionFirstLine + " |\n  ";
                    }).collect(Collectors.joining());
                message +=
                    "| Author | Description |\n" +
                        "|-|-|\n" +
                        changeTableRows;
            }
        }

        return message;
    }

}
