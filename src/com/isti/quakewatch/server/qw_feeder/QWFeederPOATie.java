package com.isti.quakewatch.server.qw_feeder;

/**
 * Interface definition: QWFeeder.
 * 
 * @author OpenORB Compiler
 */
public class QWFeederPOATie extends QWFeederPOA
{

    //
    // Private reference to implementation object
    //
    private QWFeederOperations _tie;

    //
    // Private reference to POA
    //
    private org.omg.PortableServer.POA _poa;

    /**
     * Constructor
     */
    public QWFeederPOATie(QWFeederOperations tieObject)
    {
        _tie = tieObject;
    }

    /**
     * Constructor
     */
    public QWFeederPOATie(QWFeederOperations tieObject, org.omg.PortableServer.POA poa)
    {
        _tie = tieObject;
        _poa = poa;
    }

    /**
     * Get the delegate
     */
    public QWFeederOperations _delegate()
    {
        return _tie;
    }

    /**
     * Set the delegate
     */
    public void _delegate(QWFeederOperations delegate_)
    {
        _tie = delegate_;
    }

    /**
     * _default_POA method
     */
    public org.omg.PortableServer.POA _default_POA()
    {
        if (_poa != null)
            return _poa;
        else
            return super._default_POA();
    }

    /**
     * Operation sendSourcedMsg
     */
    public boolean sendSourcedMsg(String messageStr, String feederSourceHostStr, long feederSrcHostMsgIdNum)
    {
        return _tie.sendSourcedMsg( messageStr,  feederSourceHostStr,  feederSrcHostMsgIdNum);
    }

    /**
     * Operation sendSourcedDomainTypeMsg
     */
    public boolean sendSourcedDomainTypeMsg(String domainStr, String typeStr, String messageStr, String feederSourceHostStr, long feederSrcHostMsgIdNum)
    {
        return _tie.sendSourcedDomainTypeMsg( domainStr,  typeStr,  messageStr,  feederSourceHostStr,  feederSrcHostMsgIdNum);
    }

    /**
     * Operation sendSourcedDomainTypeNameMsg
     */
    public boolean sendSourcedDomainTypeNameMsg(String domainStr, String typeStr, String nameStr, String messageStr, String feederSourceHostStr, long feederSrcHostMsgIdNum)
    {
        return _tie.sendSourcedDomainTypeNameMsg( domainStr,  typeStr,  nameStr,  messageStr,  feederSourceHostStr,  feederSrcHostMsgIdNum);
    }

    /**
     * Operation sendMessage
     */
    public boolean sendMessage(String messageStr)
    {
        return _tie.sendMessage( messageStr);
    }

    /**
     * Operation sendDomainTypeMessage
     */
    public boolean sendDomainTypeMessage(String domainStr, String typeStr, String messageStr)
    {
        return _tie.sendDomainTypeMessage( domainStr,  typeStr,  messageStr);
    }

    /**
     * Operation sendDomainTypeNameMessage
     */
    public boolean sendDomainTypeNameMessage(String domainStr, String typeStr, String nameStr, String messageStr)
    {
        return _tie.sendDomainTypeNameMessage( domainStr,  typeStr,  nameStr,  messageStr);
    }

}
