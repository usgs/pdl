package com.isti.quakewatch.server.qw_feeder;

/** 
 * Helper class for : QWFeeder
 *  
 * @author OpenORB Compiler
 */ 
public class QWFeederHelper
{
    /**
     * Insert QWFeeder into an any
     * @param a an any
     * @param t QWFeeder value
     */
    public static void insert(org.omg.CORBA.Any a, com.isti.quakewatch.server.qw_feeder.QWFeeder t)
    {
        a.insert_Object(t , type());
    }

    /**
     * Extract QWFeeder from an any
     *
     * @param a an any
     * @return the extracted QWFeeder value
     */
    public static com.isti.quakewatch.server.qw_feeder.QWFeeder extract( org.omg.CORBA.Any a )
    {
        if ( !a.type().equivalent( type() ) )
        {
            throw new org.omg.CORBA.MARSHAL();
        }
        try
        {
            return com.isti.quakewatch.server.qw_feeder.QWFeederHelper.narrow( a.extract_Object() );
        }
        catch ( final org.omg.CORBA.BAD_PARAM e )
        {
            throw new org.omg.CORBA.MARSHAL(e.getMessage());
        }
    }

    //
    // Internal TypeCode value
    //
    private static org.omg.CORBA.TypeCode _tc = null;

    /**
     * Return the QWFeeder TypeCode
     * @return a TypeCode
     */
    public static org.omg.CORBA.TypeCode type()
    {
        if (_tc == null) {
            org.omg.CORBA.ORB orb = org.omg.CORBA.ORB.init();
            _tc = orb.create_interface_tc( id(), "QWFeeder" );
        }
        return _tc;
    }

    /**
     * Return the QWFeeder IDL ID
     * @return an ID
     */
    public static String id()
    {
        return _id;
    }

    private final static String _id = "IDL:com/isti/quakewatch/server/qw_feeder/QWFeeder:1.0";

    /**
     * Read QWFeeder from a marshalled stream
     * @param istream the input stream
     * @return the readed QWFeeder value
     */
    public static com.isti.quakewatch.server.qw_feeder.QWFeeder read(org.omg.CORBA.portable.InputStream istream)
    {
        return(com.isti.quakewatch.server.qw_feeder.QWFeeder)istream.read_Object(com.isti.quakewatch.server.qw_feeder._QWFeederStub.class);
    }

    /**
     * Write QWFeeder into a marshalled stream
     * @param ostream the output stream
     * @param value QWFeeder value
     */
    public static void write(org.omg.CORBA.portable.OutputStream ostream, com.isti.quakewatch.server.qw_feeder.QWFeeder value)
    {
        ostream.write_Object((org.omg.CORBA.portable.ObjectImpl)value);
    }

    /**
     * Narrow CORBA::Object to QWFeeder
     * @param obj the CORBA Object
     * @return QWFeeder Object
     */
    public static QWFeeder narrow(org.omg.CORBA.Object obj)
    {
        if (obj == null)
            return null;
        if (obj instanceof QWFeeder)
            return (QWFeeder)obj;

        if (obj._is_a(id()))
        {
            _QWFeederStub stub = new _QWFeederStub();
            stub._set_delegate(((org.omg.CORBA.portable.ObjectImpl)obj)._get_delegate());
            return stub;
        }

        throw new org.omg.CORBA.BAD_PARAM();
    }

    /**
     * Unchecked Narrow CORBA::Object to QWFeeder
     * @param obj the CORBA Object
     * @return QWFeeder Object
     */
    public static QWFeeder unchecked_narrow(org.omg.CORBA.Object obj)
    {
        if (obj == null)
            return null;
        if (obj instanceof QWFeeder)
            return (QWFeeder)obj;

        _QWFeederStub stub = new _QWFeederStub();
        stub._set_delegate(((org.omg.CORBA.portable.ObjectImpl)obj)._get_delegate());
        return stub;

    }

}
