package com.petcarehub;
import com.petcarehub.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.Mockito.*;
class NotificationJobFailureTest {
    @Test void failedDispatchRecordsOnlyFailureFlagAndRetriesBatch(){
        var dispatch=mock(NotificationDispatch.class);
        var monitor=mock(JobMonitor.class);
        var jobs=new NotificationJobs(dispatch,mock(CustomerReminders.class),monitor);
        ReflectionTestUtils.setField(jobs,"enabled",true);
        when(monitor.start("NOTIFICATION_DISPATCH")).thenReturn(1L);
        when(dispatch.next()).thenReturn(9L);
        when(dispatch.process(9L)).thenThrow(new IllegalStateException("secret-password-do-not-persist"));
        jobs.dispatch();
        verify(dispatch).failed(9L);
        verify(monitor).finish(1L,0,true);
    }
    @Test void failedReminderDoesNotLeakExceptionToTelemetry(){
        var reminder=mock(CustomerReminders.class);
        var monitor=mock(JobMonitor.class);
        var jobs=new NotificationJobs(mock(NotificationDispatch.class),reminder,monitor);
        ReflectionTestUtils.setField(jobs,"remindersEnabled",true);
        when(monitor.start("CUSTOMER_REMINDERS")).thenReturn(2L);
        when(reminder.run()).thenThrow(new IllegalStateException("jdbc:password=secret"));
        jobs.reminders();
        verify(monitor).finish(2L,0,true);
    }
}
