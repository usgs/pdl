




import gov.usgs.util.JDBCUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.ResultSetMetaData;


public class SqlToCsv {
	
	public static final String JDBC_DRIVER = "oracle.jdbc.OracleDriver";
	public static final String JDBC_URL = "jdbc:oracle:thin:gs_owner/Inde0704*2010@gldintdb01:1521:ghsct1";
	public static final String SQL = "select name_s, name_m, name_e, name_h, name_l, area, sdo_util.to_wktgeometry(shape), fe_reg_num, priority, dataset from fe_all_view";
	
	public static void main(final String[] args) throws Exception {
		Connection conn = JDBCUtils.getConnection(JDBC_DRIVER, JDBC_URL);
		Statement statement = conn.createStatement();
		ResultSet rs = statement.executeQuery(SQL);
		ResultSetMetaData rsmd = rs.getMetaData();

		StringBuffer buf = new StringBuffer();
		int numColumns = rsmd.getColumnCount();
/*
	//output csv headers
		buf.append(rsmd.getColumnName(1));
		for (int i = 1; i < numColumns; i++) {
			buf.append(",").append(rsmd.getColumnName(i+1));
		}
		System.out.println(buf.toString());
*/
		while (rs.next()) {
			buf = new StringBuffer();
			//output csv data
			String value = rs.getString(1);
			if (value != null) {
				value = "'" + value.replace("'", "\\'") + "'";
			}
			buf.append(value);

			for (int i = 1; i < numColumns; i++) {
				value = rs.getString(i+1);
				if (value != null) {
					value = "'" + value.replace("'", "\\'") + "'";
				}
				buf.append(",").append(value);
			}
			System.out.println(buf.toString());
		}
	}

}