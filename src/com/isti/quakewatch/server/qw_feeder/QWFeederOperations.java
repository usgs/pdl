package com.isti.quakewatch.server.qw_feeder;

/**
 * Interface definition: QWFeeder.
 * 
 * @author OpenORB Compiler
 */
public interface QWFeederOperations
{
    /**
     * Sends a data event message.  If the event data does not
     * begin with a "DataMessage" XML element then the data will
     * be surrounded with one.  The "sendSourced..." methods are
     * preferred because the feeder-source host name and message
     * number are used for improved message tracking.
     * @param  messageStr the data event message string.
     * @param  feederSourceHostStr the data-source host string for
     * the message.
     * @param  feederSrcHostMsgIdNum the message-ID number from the
     * data source (positive value incremented after each message).
     * @return  true after the message has been successfully stored
     * and processed; false if an error occurred.
     */
    public boolean sendSourcedMsg(String messageStr, String feederSourceHostStr, long feederSrcHostMsgIdNum);

    /**
     * Sends a data event message.  If the event data does not
     * begin with a "DataMessage" XML element then the data will
     * be surrounded with one.  The "sendSourced..." methods are
     * preferred because the feeder-source host name and message
     * number are used for improved message tracking.
     * @param  domainStr the domain name to use.
     * @param  typeStr the type name to use.
     * @param  messageStr the data event message string.
     * @param  feederSourceHostStr the data-source host string for
     * the message.
     * @param  feederSrcHostMsgIdNum the message-ID number from the
     * data source (positive value incremented after each message).
     * @return  true after the message has been successfully stored
     * and processed; false if an error occurred.
     */
    public boolean sendSourcedDomainTypeMsg(String domainStr, String typeStr, String messageStr, String feederSourceHostStr, long feederSrcHostMsgIdNum);

    /**
     * Sends a data event message.  If the event data does not
     * begin with a "DataMessage" XML element then the data will
     * be surrounded with one.  The "sendSourced..." methods are
     * preferred because the feeder-source host name and message
     * number are used for improved message tracking.
     * @param  domainStr the domain name to use.
     * @param  typeStr the type name to use.
     * @param  nameStr the event name to use.
     * @param  messageStr the data event message string.
     * @param  feederSourceHostStr the data-source host string for
     * the message.
     * @param  feederSrcHostMsgIdNum the message-ID number from the
     * data source (positive value incremented after each message).
     * @return  true after the message has been successfully stored
     * and processed; false if an error occurred.
     */
    public boolean sendSourcedDomainTypeNameMsg(String domainStr, String typeStr, String nameStr, String messageStr, String feederSourceHostStr, long feederSrcHostMsgIdNum);

    /**
     * Sends a data event message.  If the event data does not
     * begin with a "DataMessage" XML element then the data will
     * be surrounded with one.  The "sendSourced..." methods are
     * preferred because the feeder-source host name and message
     * number are used for improved message tracking.
     * @param  messageStr the data event message string.
     * @return  true after the message has been successfully stored
     * and processed; false if an error occurred.
     */
    public boolean sendMessage(String messageStr);

    /**
     * Sends a data event message.  If the event data does not
     * begin with a "DataMessage" XML element then the data will
     * be surrounded with one.  The "sendSourced..." methods are
     * preferred because the feeder-source host name and message
     * number are used for improved message tracking.
     * @param  domainStr the domain name to use.
     * @param  typeStr the type name to use.
     * @param  messageStr the data event message string.
     * @return  true after the message has been successfully stored
     * and processed; false if an error occurred.
     */
    public boolean sendDomainTypeMessage(String domainStr, String typeStr, String messageStr);

    /**
     * Sends a data event message.  If the event data does not
     * begin with a "DataMessage" XML element then the data will
     * be surrounded with one.  The "sendSourced..." methods are
     * preferred because the feeder-source host name and message
     * number are used for improved message tracking.
     * @param  domainStr the domain name to use.
     * @param  typeStr the type name to use.
     * @param  nameStr the event name to use.
     * @param  messageStr the data event message string.
     * @return  true after the message has been successfully stored
     * and processed; false if an error occurred.
     */
    public boolean sendDomainTypeNameMessage(String domainStr, String typeStr, String nameStr, String messageStr);

}
