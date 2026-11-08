package com.fytsmart.dashboard;

import android.service.notification.NotificationListenerService;

public class MediaListener extends NotificationListenerService {
    // El permiso de acceso a notificaciones habilita MediaSessionManager.getActiveSessions().
    @Override public void onListenerConnected() { super.onListenerConnected(); }
}
