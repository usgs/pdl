package com.isti.quakewatch.server.qw_feeder;

/**
 * Interface definition: QWFeeder.
 * 
 * @author OpenORB Compiler
 */
public abstract class QWFeederPOA extends org.omg.PortableServer.Servant
        implements QWFeederOperations, org.omg.CORBA.portable.InvokeHandler
{
    public QWFeeder _this()
    {
        return QWFeederHelper.narrow(_this_object());
    }

    public QWFeeder _this(org.omg.CORBA.ORB orb)
    {
        return QWFeederHelper.narrow(_this_object(orb));
    }

    private static String [] _ids_list =
    {
        "IDL:com/isti/quakewatch/server/qw_feeder/QWFeeder:1.0"
    };

    public String[] _all_interfaces(org.omg.PortableServer.POA poa, byte [] objectId)
    {
        return _ids_list;
    }

    public final org.omg.CORBA.portable.OutputStream _invoke(final String opName,
            final org.omg.CORBA.portable.InputStream _is,
            final org.omg.CORBA.portable.ResponseHandler handler)
    {

        if (opName.equals("sendDomainTypeMessage")) {
                return _invoke_sendDomainTypeMessage(_is, handler);
        } else if (opName.equals("sendDomainTypeNameMessage")) {
                return _invoke_sendDomainTypeNameMessage(_is, handler);
        } else if (opName.equals("sendMessage")) {
                return _invoke_sendMessage(_is, handler);
        } else if (opName.equals("sendSourcedDomainTypeMsg")) {
                return _invoke_sendSourcedDomainTypeMsg(_is, handler);
        } else if (opName.equals("sendSourcedDomainTypeNameMsg")) {
                return _invoke_sendSourcedDomainTypeNameMsg(_is, handler);
        } else if (opName.equals("sendSourcedMsg")) {
                return _invoke_sendSourcedMsg(_is, handler);
        } else {
            throw new org.omg.CORBA.BAD_OPERATION(opName);
        }
    }

    // helper methods
    private org.omg.CORBA.portable.OutputStream _invoke_sendSourcedMsg(
            final org.omg.CORBA.portable.InputStream _is,
            final org.omg.CORBA.portable.ResponseHandler handler) {
        org.omg.CORBA.portable.OutputStream _output;
        String arg0_in = _is.read_string();
        String arg1_in = _is.read_string();
        long arg2_in = _is.read_longlong();

        boolean _arg_result = sendSourcedMsg(arg0_in, arg1_in, arg2_in);

        _output = handler.createReply();
        _output.write_boolean(_arg_result);

        return _output;
    }

    private org.omg.CORBA.portable.OutputStream _invoke_sendSourcedDomainTypeMsg(
            final org.omg.CORBA.portable.InputStream _is,
            final org.omg.CORBA.portable.ResponseHandler handler) {
        org.omg.CORBA.portable.OutputStream _output;
        String arg0_in = _is.read_string();
        String arg1_in = _is.read_string();
        String arg2_in = _is.read_string();
        String arg3_in = _is.read_string();
        long arg4_in = _is.read_longlong();

        boolean _arg_result = sendSourcedDomainTypeMsg(arg0_in, arg1_in, arg2_in, arg3_in, arg4_in);

        _output = handler.createReply();
        _output.write_boolean(_arg_result);

        return _output;
    }

    private org.omg.CORBA.portable.OutputStream _invoke_sendSourcedDomainTypeNameMsg(
            final org.omg.CORBA.portable.InputStream _is,
            final org.omg.CORBA.portable.ResponseHandler handler) {
        org.omg.CORBA.portable.OutputStream _output;
        String arg0_in = _is.read_string();
        String arg1_in = _is.read_string();
        String arg2_in = _is.read_string();
        String arg3_in = _is.read_string();
        String arg4_in = _is.read_string();
        long arg5_in = _is.read_longlong();

        boolean _arg_result = sendSourcedDomainTypeNameMsg(arg0_in, arg1_in, arg2_in, arg3_in, arg4_in, arg5_in);

        _output = handler.createReply();
        _output.write_boolean(_arg_result);

        return _output;
    }

    private org.omg.CORBA.portable.OutputStream _invoke_sendMessage(
            final org.omg.CORBA.portable.InputStream _is,
            final org.omg.CORBA.portable.ResponseHandler handler) {
        org.omg.CORBA.portable.OutputStream _output;
        String arg0_in = _is.read_string();

        boolean _arg_result = sendMessage(arg0_in);

        _output = handler.createReply();
        _output.write_boolean(_arg_result);

        return _output;
    }

    private org.omg.CORBA.portable.OutputStream _invoke_sendDomainTypeMessage(
            final org.omg.CORBA.portable.InputStream _is,
            final org.omg.CORBA.portable.ResponseHandler handler) {
        org.omg.CORBA.portable.OutputStream _output;
        String arg0_in = _is.read_string();
        String arg1_in = _is.read_string();
        String arg2_in = _is.read_string();

        boolean _arg_result = sendDomainTypeMessage(arg0_in, arg1_in, arg2_in);

        _output = handler.createReply();
        _output.write_boolean(_arg_result);

        return _output;
    }

    private org.omg.CORBA.portable.OutputStream _invoke_sendDomainTypeNameMessage(
            final org.omg.CORBA.portable.InputStream _is,
            final org.omg.CORBA.portable.ResponseHandler handler) {
        org.omg.CORBA.portable.OutputStream _output;
        String arg0_in = _is.read_string();
        String arg1_in = _is.read_string();
        String arg2_in = _is.read_string();
        String arg3_in = _is.read_string();

        boolean _arg_result = sendDomainTypeNameMessage(arg0_in, arg1_in, arg2_in, arg3_in);

        _output = handler.createReply();
        _output.write_boolean(_arg_result);

        return _output;
    }

}
