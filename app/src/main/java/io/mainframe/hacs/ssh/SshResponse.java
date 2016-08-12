package io.mainframe.hacs.ssh;

/**
 * Created by holger on 28.11.15.
 */
public interface SshResponse<T> {
    void processFinish(T response);
}
