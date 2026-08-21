package com.groupsync.backend.user;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class Phase6ProductSurfaceContractTest {
    @Test
    void primaryNavigationContainsExactlyTheFourCoreDestinations() {
        String app = frontend("App.tsx");
        String navItems = app.substring(app.indexOf("const navItems"), app.indexOf("return <div"));
        assertTrue(navItems.contains("label: 'Home'"));
        assertTrue(navItems.contains("label: 'Library'"));
        assertTrue(navItems.contains("label: 'Ask'"));
        assertTrue(navItems.contains("label: 'Focus'"));
        assertFalse(navItems.contains("Insights"));
        assertFalse(navItems.contains("Guide"));
    }

    @Test
    void insightsRouteRemainsAvailableButHomeDoesNotPromoteIt() {
        assertTrue(frontend("App.tsx").contains("path=\"/insights\""));
        assertFalse(frontend("pages/KnowledgeHomePage.tsx").contains("to=\"/insights\""));
    }

    @Test
    void guideDocumentsCurrentBoundariesAndDoesNotAdvertiseLegacyIntegrations() {
        String guide = frontend("pages/KnowledgeGuidePage.tsx");
        assertTrue(guide.contains("reciprocal rank fusion"));
        assertTrue(guide.contains("server-sent event trace"));
        assertTrue(guide.contains("cannot report an exact Gemini balance, quota, or reset time"));
        assertTrue(guide.contains("preserving work you have already completed"));
        assertFalse(guide.contains("Google Drive"));
        assertFalse(guide.contains("GroupSync"));
    }

    @Test
    void profileKeepsTimezoneOutOfTheVisibleFormAndUsesSectionNotices() {
        String profile = frontend("pages/ProfilePage.tsx");
        assertFalse(profile.contains("profile-zone"));
        assertFalse(profile.contains(">Timezone<"));
        assertTrue(profile.contains("accountNotice"));
        assertTrue(profile.contains("avatarNotice"));
        assertTrue(profile.contains("passwordNotice"));
    }

    @Test
    void profileSupportsAvatarPreviewCancelReplacementAndRemoval() {
        String profile = frontend("pages/ProfilePage.tsx");
        assertTrue(profile.contains("Selected avatar preview"));
        assertTrue(profile.contains("cancelPreview"));
        assertTrue(profile.contains("uploadAvatar(selectedAvatar)"));
        assertTrue(profile.contains("deleteAvatar()"));
        assertTrue(profile.contains("avatarUrl: null"));
    }

    private String frontend(String relativePath) {
        try {
            return Files.readString(Path.of("../frontend/src").resolve(relativePath), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
