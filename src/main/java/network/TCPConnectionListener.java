package network;

public interface TCPConnectionListener {
    void onConnectionReady(TCPConnection connection);
    void onReceiveString(TCPConnection connection, String message);
    void onDisconnect(TCPConnection connection);
    void onException(TCPConnection connection, Exception e);
}
