package com.isti.quakewatch.server.qw_feeder;

/**
 * Interface definition: QWFeeder.
 * 
 * @author OpenORB Compiler
 */
public class _QWFeederStub extends org.omg.CORBA.portable.ObjectImpl
        implements QWFeeder
{
    /** Serialization id. */
	private static final long serialVersionUID = 1L;
	
	static final String[] _ids_list =
    {
        "IDL:com/isti/quakewatch/server/qw_feeder/QWFeeder:1.0"
    };

    public String[] _ids()
    {
     return _ids_list;
    }

    private final static Class<QWFeederOperations> _opsClass = com.isti.quakewatch.server.qw_feeder.QWFeederOperations.class;

    /**
     * Operation sendSourcedMsg
     */
    public boolean sendSourcedMsg(String messageStr, String feederSourceHostStr, long feederSrcHostMsgIdNum)
    {
        while(true)
        {
            if (!this._is_local())
            {
                org.omg.CORBA.portable.InputStream _input = null;
                try
                {
                    org.omg.CORBA.portable.OutputStream _output = this._request("sendSourcedMsg",true);
                    _output.write_string(messageStr);
                    _output.write_string(feederSourceHostStr);
                    _output.write_longlong(feederSrcHostMsgIdNum);
                    _input = this._invoke(_output);
                    boolean _arg_ret = _input.read_boolean();
                    return _arg_ret;
                }
                catch(org.omg.CORBA.portable.RemarshalException _exception)
                {
                    continue;
                }
                catch(org.omg.CORBA.portable.ApplicationException _exception)
                {
                    String _exception_id = _exception.getId();
                    throw new org.omg.CORBA.UNKNOWN("Unexpected User Exception: "+ _exception_id);
                }
                finally
                {
                    this._releaseReply(_input);
                }
            }
            else
            {
                org.omg.CORBA.portable.ServantObject _so = _servant_preinvoke("sendSourcedMsg",_opsClass);
                if (_so == null)
                   continue;
                com.isti.quakewatch.server.qw_feeder.QWFeederOperations _self = (com.isti.quakewatch.server.qw_feeder.QWFeederOperations) _so.servant;
                try
                {
                    return _self.sendSourcedMsg( messageStr,  feederSourceHostStr,  feederSrcHostMsgIdNum);
                }
                finally
                {
                    _servant_postinvoke(_so);
                }
            }
        }
    }

    /**
     * Operation sendSourcedDomainTypeMsg
     */
    public boolean sendSourcedDomainTypeMsg(String domainStr, String typeStr, String messageStr, String feederSourceHostStr, long feederSrcHostMsgIdNum)
    {
        while(true)
        {
            if (!this._is_local())
            {
                org.omg.CORBA.portable.InputStream _input = null;
                try
                {
                    org.omg.CORBA.portable.OutputStream _output = this._request("sendSourcedDomainTypeMsg",true);
                    _output.write_string(domainStr);
                    _output.write_string(typeStr);
                    _output.write_string(messageStr);
                    _output.write_string(feederSourceHostStr);
                    _output.write_longlong(feederSrcHostMsgIdNum);
                    _input = this._invoke(_output);
                    boolean _arg_ret = _input.read_boolean();
                    return _arg_ret;
                }
                catch(org.omg.CORBA.portable.RemarshalException _exception)
                {
                    continue;
                }
                catch(org.omg.CORBA.portable.ApplicationException _exception)
                {
                    String _exception_id = _exception.getId();
                    throw new org.omg.CORBA.UNKNOWN("Unexpected User Exception: "+ _exception_id);
                }
                finally
                {
                    this._releaseReply(_input);
                }
            }
            else
            {
                org.omg.CORBA.portable.ServantObject _so = _servant_preinvoke("sendSourcedDomainTypeMsg",_opsClass);
                if (_so == null)
                   continue;
                com.isti.quakewatch.server.qw_feeder.QWFeederOperations _self = (com.isti.quakewatch.server.qw_feeder.QWFeederOperations) _so.servant;
                try
                {
                    return _self.sendSourcedDomainTypeMsg( domainStr,  typeStr,  messageStr,  feederSourceHostStr,  feederSrcHostMsgIdNum);
                }
                finally
                {
                    _servant_postinvoke(_so);
                }
            }
        }
    }

    /**
     * Operation sendSourcedDomainTypeNameMsg
     */
    public boolean sendSourcedDomainTypeNameMsg(String domainStr, String typeStr, String nameStr, String messageStr, String feederSourceHostStr, long feederSrcHostMsgIdNum)
    {
        while(true)
        {
            if (!this._is_local())
            {
                org.omg.CORBA.portable.InputStream _input = null;
                try
                {
                    org.omg.CORBA.portable.OutputStream _output = this._request("sendSourcedDomainTypeNameMsg",true);
                    _output.write_string(domainStr);
                    _output.write_string(typeStr);
                    _output.write_string(nameStr);
                    _output.write_string(messageStr);
                    _output.write_string(feederSourceHostStr);
                    _output.write_longlong(feederSrcHostMsgIdNum);
                    _input = this._invoke(_output);
                    boolean _arg_ret = _input.read_boolean();
                    return _arg_ret;
                }
                catch(org.omg.CORBA.portable.RemarshalException _exception)
                {
                    continue;
                }
                catch(org.omg.CORBA.portable.ApplicationException _exception)
                {
                    String _exception_id = _exception.getId();
                    throw new org.omg.CORBA.UNKNOWN("Unexpected User Exception: "+ _exception_id);
                }
                finally
                {
                    this._releaseReply(_input);
                }
            }
            else
            {
                org.omg.CORBA.portable.ServantObject _so = _servant_preinvoke("sendSourcedDomainTypeNameMsg",_opsClass);
                if (_so == null)
                   continue;
                com.isti.quakewatch.server.qw_feeder.QWFeederOperations _self = (com.isti.quakewatch.server.qw_feeder.QWFeederOperations) _so.servant;
                try
                {
                    return _self.sendSourcedDomainTypeNameMsg( domainStr,  typeStr,  nameStr,  messageStr,  feederSourceHostStr,  feederSrcHostMsgIdNum);
                }
                finally
                {
                    _servant_postinvoke(_so);
                }
            }
        }
    }

    /**
     * Operation sendMessage
     */
    public boolean sendMessage(String messageStr)
    {
        while(true)
        {
            if (!this._is_local())
            {
                org.omg.CORBA.portable.InputStream _input = null;
                try
                {
                    org.omg.CORBA.portable.OutputStream _output = this._request("sendMessage",true);
                    _output.write_string(messageStr);
                    _input = this._invoke(_output);
                    boolean _arg_ret = _input.read_boolean();
                    return _arg_ret;
                }
                catch(org.omg.CORBA.portable.RemarshalException _exception)
                {
                    continue;
                }
                catch(org.omg.CORBA.portable.ApplicationException _exception)
                {
                    String _exception_id = _exception.getId();
                    throw new org.omg.CORBA.UNKNOWN("Unexpected User Exception: "+ _exception_id);
                }
                finally
                {
                    this._releaseReply(_input);
                }
            }
            else
            {
                org.omg.CORBA.portable.ServantObject _so = _servant_preinvoke("sendMessage",_opsClass);
                if (_so == null)
                   continue;
                com.isti.quakewatch.server.qw_feeder.QWFeederOperations _self = (com.isti.quakewatch.server.qw_feeder.QWFeederOperations) _so.servant;
                try
                {
                    return _self.sendMessage( messageStr);
                }
                finally
                {
                    _servant_postinvoke(_so);
                }
            }
        }
    }

    /**
     * Operation sendDomainTypeMessage
     */
    public boolean sendDomainTypeMessage(String domainStr, String typeStr, String messageStr)
    {
        while(true)
        {
            if (!this._is_local())
            {
                org.omg.CORBA.portable.InputStream _input = null;
                try
                {
                    org.omg.CORBA.portable.OutputStream _output = this._request("sendDomainTypeMessage",true);
                    _output.write_string(domainStr);
                    _output.write_string(typeStr);
                    _output.write_string(messageStr);
                    _input = this._invoke(_output);
                    boolean _arg_ret = _input.read_boolean();
                    return _arg_ret;
                }
                catch(org.omg.CORBA.portable.RemarshalException _exception)
                {
                    continue;
                }
                catch(org.omg.CORBA.portable.ApplicationException _exception)
                {
                    String _exception_id = _exception.getId();
                    throw new org.omg.CORBA.UNKNOWN("Unexpected User Exception: "+ _exception_id);
                }
                finally
                {
                    this._releaseReply(_input);
                }
            }
            else
            {
                org.omg.CORBA.portable.ServantObject _so = _servant_preinvoke("sendDomainTypeMessage",_opsClass);
                if (_so == null)
                   continue;
                com.isti.quakewatch.server.qw_feeder.QWFeederOperations _self = (com.isti.quakewatch.server.qw_feeder.QWFeederOperations) _so.servant;
                try
                {
                    return _self.sendDomainTypeMessage( domainStr,  typeStr,  messageStr);
                }
                finally
                {
                    _servant_postinvoke(_so);
                }
            }
        }
    }

    /**
     * Operation sendDomainTypeNameMessage
     */
    public boolean sendDomainTypeNameMessage(String domainStr, String typeStr, String nameStr, String messageStr)
    {
        while(true)
        {
            if (!this._is_local())
            {
                org.omg.CORBA.portable.InputStream _input = null;
                try
                {
                    org.omg.CORBA.portable.OutputStream _output = this._request("sendDomainTypeNameMessage",true);
                    _output.write_string(domainStr);
                    _output.write_string(typeStr);
                    _output.write_string(nameStr);
                    _output.write_string(messageStr);
                    _input = this._invoke(_output);
                    boolean _arg_ret = _input.read_boolean();
                    return _arg_ret;
                }
                catch(org.omg.CORBA.portable.RemarshalException _exception)
                {
                    continue;
                }
                catch(org.omg.CORBA.portable.ApplicationException _exception)
                {
                    String _exception_id = _exception.getId();
                    throw new org.omg.CORBA.UNKNOWN("Unexpected User Exception: "+ _exception_id);
                }
                finally
                {
                    this._releaseReply(_input);
                }
            }
            else
            {
                org.omg.CORBA.portable.ServantObject _so = _servant_preinvoke("sendDomainTypeNameMessage",_opsClass);
                if (_so == null)
                   continue;
                com.isti.quakewatch.server.qw_feeder.QWFeederOperations _self = (com.isti.quakewatch.server.qw_feeder.QWFeederOperations) _so.servant;
                try
                {
                    return _self.sendDomainTypeNameMessage( domainStr,  typeStr,  nameStr,  messageStr);
                }
                finally
                {
                    _servant_postinvoke(_so);
                }
            }
        }
    }

}
