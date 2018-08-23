package com.isti.quakewatch.server.qw_feeder;

/**
 * Holder class for : QWFeeder
 * 
 * @author OpenORB Compiler
 */
final public class QWFeederHolder
        implements org.omg.CORBA.portable.Streamable
{
    /**
     * Internal QWFeeder value
     */
    public com.isti.quakewatch.server.qw_feeder.QWFeeder value;

    /**
     * Default constructor
     */
    public QWFeederHolder()
    { }

    /**
     * Constructor with value initialisation
     * @param initial the initial value
     */
    public QWFeederHolder(com.isti.quakewatch.server.qw_feeder.QWFeeder initial)
    {
        value = initial;
    }

    /**
     * Read QWFeeder from a marshalled stream
     * @param istream the input stream
     */
    public void _read(org.omg.CORBA.portable.InputStream istream)
    {
        value = QWFeederHelper.read(istream);
    }

    /**
     * Write QWFeeder into a marshalled stream
     * @param ostream the output stream
     */
    public void _write(org.omg.CORBA.portable.OutputStream ostream)
    {
        QWFeederHelper.write(ostream,value);
    }

    /**
     * Return the QWFeeder TypeCode
     * @return a TypeCode
     */
    public org.omg.CORBA.TypeCode _type()
    {
        return QWFeederHelper.type();
    }

}
