package com.ebay.dss.zds.collection;

import java.lang.reflect.*;
import java.sql.*;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Created by tatian on 2020-07-07.
 */

// todo: add more function in this class
public class ResultSetWrap implements ResultSet {

  private ResultSet rs;

  public ResultSetWrap(ResultSet rs) {
    this.rs = rs;
  }

  @Override
  public void updateBytes(java.lang.String arg0,byte[] arg1) throws java.sql.SQLException {
    rs.updateBytes(arg0, arg1);
  }

  @Override
  public void updateBytes(int arg0,byte[] arg1) throws java.sql.SQLException {
    rs.updateBytes(arg0, arg1);
  }

  @Override
  public java.lang.Object getObject(int arg0,java.util.Map<java.lang.String, java.lang.Class<?>> arg1) throws java.sql.SQLException {
    return rs.getObject(arg0, arg1);
  }

  @Override
  public java.lang.Object getObject(java.lang.String arg0) throws java.sql.SQLException {
    return rs.getObject(arg0);
  }

  @Override
  public java.lang.Object getObject(int arg0) throws java.sql.SQLException {
    return rs.getObject(arg0);
  }

  @Override
  public java.lang.Object getObject(java.lang.String arg0,java.util.Map<java.lang.String, java.lang.Class<?>> arg1) throws java.sql.SQLException {
    return rs.getObject(arg0, arg1);
  }

  @Override
  public <T> T getObject(int arg0,java.lang.Class<T> arg1) throws java.sql.SQLException {
    return rs.getObject(arg0, arg1);
  }

  @Override
  public <T> T getObject(java.lang.String arg0,java.lang.Class<T> arg1) throws java.sql.SQLException {
    return rs.getObject(arg0, arg1);
  }

  @Override
  public boolean getBoolean(int arg0) throws java.sql.SQLException {
    return rs.getBoolean(arg0);
  }

  @Override
  public boolean getBoolean(java.lang.String arg0) throws java.sql.SQLException {
    return rs.getBoolean(arg0);
  }

  @Override
  public byte getByte(java.lang.String arg0) throws java.sql.SQLException {
    return rs.getByte(arg0);
  }

  @Override
  public byte getByte(int arg0) throws java.sql.SQLException {
    return rs.getByte(arg0);
  }

  @Override
  public short getShort(int arg0) throws java.sql.SQLException {
    return rs.getShort(arg0);
  }

  @Override
  public short getShort(java.lang.String arg0) throws java.sql.SQLException {
    return rs.getShort(arg0);
  }

  @Override
  public int getInt(int arg0) throws java.sql.SQLException {
    return rs.getInt(arg0);
  }

  @Override
  public int getInt(java.lang.String arg0) throws java.sql.SQLException {
    return rs.getInt(arg0);
  }

  @Override
  public long getLong(java.lang.String arg0) throws java.sql.SQLException {
    return rs.getLong(arg0);
  }

  @Override
  public long getLong(int arg0) throws java.sql.SQLException {
    return rs.getLong(arg0);
  }

  @Override
  public float getFloat(java.lang.String arg0) throws java.sql.SQLException {
    return rs.getFloat(arg0);
  }

  @Override
  public float getFloat(int arg0) throws java.sql.SQLException {
    return rs.getFloat(arg0);
  }

  @Override
  public double getDouble(java.lang.String arg0) throws java.sql.SQLException {
    return rs.getDouble(arg0);
  }

  @Override
  public double getDouble(int arg0) throws java.sql.SQLException {
    return rs.getDouble(arg0);
  }

  @Override
  public byte[] getBytes(int arg0) throws java.sql.SQLException {
    return rs.getBytes(arg0);
  }

  @Override
  public byte[] getBytes(java.lang.String arg0) throws java.sql.SQLException {
    return rs.getBytes(arg0);
  }

  @Override
  public boolean next() throws java.sql.SQLException {
    return rs.next();
  }

  @Override
  public java.sql.Array getArray(java.lang.String arg0) throws java.sql.SQLException {
    return rs.getArray(arg0);
  }

  @Override
  public java.sql.Array getArray(int arg0) throws java.sql.SQLException {
    return rs.getArray(arg0);
  }

  @Override
  public java.net.URL getURL(java.lang.String arg0) throws java.sql.SQLException {
    return rs.getURL(arg0);
  }

  @Override
  public java.net.URL getURL(int arg0) throws java.sql.SQLException {
    return rs.getURL(arg0);
  }

  @Override
  public boolean first() throws java.sql.SQLException {
    return rs.first();
  }

  @Override
  public void close() throws java.sql.SQLException {
    rs.close();
  }

  @Override
  public int getType() throws java.sql.SQLException {
    return rs.getType();
  }

  @Override
  public java.sql.Ref getRef(java.lang.String arg0) throws java.sql.SQLException {
    return rs.getRef(arg0);
  }

  @Override
  public java.sql.Ref getRef(int arg0) throws java.sql.SQLException {
    return rs.getRef(arg0);
  }

  @Override
  public boolean previous() throws java.sql.SQLException {
    return rs.previous();
  }

  @Override
  public boolean isFirst() throws java.sql.SQLException {
    return rs.isFirst();
  }

  @Override
  public boolean wasNull() throws java.sql.SQLException {
    return rs.wasNull();
  }

  @Override
  public boolean isLast() throws java.sql.SQLException {
    return rs.isLast();
  }

  @Override
  public java.math.BigDecimal getBigDecimal(int arg0) throws java.sql.SQLException {
    return rs.getBigDecimal(arg0);
  }

  @Override
  public java.math.BigDecimal getBigDecimal(java.lang.String arg0) throws java.sql.SQLException {
    return rs.getBigDecimal(arg0);
  }

  @Override
  public java.math.BigDecimal getBigDecimal(java.lang.String arg0,int arg1) throws java.sql.SQLException {
    return rs.getBigDecimal(arg0, arg1);
  }

  @Override
  public java.math.BigDecimal getBigDecimal(int arg0,int arg1) throws java.sql.SQLException {
    return rs.getBigDecimal(arg0, arg1);
  }

  @Override
  public java.io.InputStream getAsciiStream(java.lang.String arg0) throws java.sql.SQLException {
    return rs.getAsciiStream(arg0);
  }

  @Override
  public java.io.InputStream getAsciiStream(int arg0) throws java.sql.SQLException {
    return rs.getAsciiStream(arg0);
  }

  @Override
  public java.io.InputStream getUnicodeStream(int arg0) throws java.sql.SQLException {
    return rs.getUnicodeStream(arg0);
  }

  @Override
  public java.io.InputStream getUnicodeStream(java.lang.String arg0) throws java.sql.SQLException {
    return rs.getUnicodeStream(arg0);
  }

  @Override
  public java.io.InputStream getBinaryStream(java.lang.String arg0) throws java.sql.SQLException {
    return rs.getBinaryStream(arg0);
  }

  @Override
  public java.io.InputStream getBinaryStream(int arg0) throws java.sql.SQLException {
    return rs.getBinaryStream(arg0);
  }

  @Override
  public java.sql.SQLWarning getWarnings() throws java.sql.SQLException {
    return rs.getWarnings();
  }

  @Override
  public void clearWarnings() throws java.sql.SQLException {
    rs.clearWarnings();
  }

  @Override
  public java.lang.String getCursorName() throws java.sql.SQLException {
    return rs.getCursorName();
  }

  @Override
  public java.sql.ResultSetMetaData getMetaData() throws java.sql.SQLException {
    return rs.getMetaData();
  }

  @Override
  public int findColumn(java.lang.String arg0) throws java.sql.SQLException {
    return rs.findColumn(arg0);
  }

  @Override
  public java.io.Reader getCharacterStream(java.lang.String arg0) throws java.sql.SQLException {
    return rs.getCharacterStream(arg0);
  }

  @Override
  public java.io.Reader getCharacterStream(int arg0) throws java.sql.SQLException {
    return rs.getCharacterStream(arg0);
  }

  @Override
  public boolean isBeforeFirst() throws java.sql.SQLException {
    return rs.isBeforeFirst();
  }

  @Override
  public boolean isAfterLast() throws java.sql.SQLException {
    return rs.isAfterLast();
  }

  @Override
  public void beforeFirst() throws java.sql.SQLException {
    rs.beforeFirst();
  }

  @Override
  public void afterLast() throws java.sql.SQLException {
    rs.afterLast();
  }

  @Override
  public int getRow() throws java.sql.SQLException {
    return rs.getRow();
  }

  @Override
  public boolean absolute(int arg0) throws java.sql.SQLException {
    return rs.absolute(arg0);
  }

  @Override
  public boolean relative(int arg0) throws java.sql.SQLException {
    return rs.relative(arg0);
  }

  @Override
  public void setFetchDirection(int arg0) throws java.sql.SQLException {
    rs.setFetchDirection(arg0);
  }

  @Override
  public int getFetchDirection() throws java.sql.SQLException {
    return rs.getFetchDirection();
  }

  @Override
  public void setFetchSize(int arg0) throws java.sql.SQLException {
    rs.setFetchSize(arg0);
  }

  @Override
  public int getFetchSize() throws java.sql.SQLException {
    return rs.getFetchSize();
  }

  @Override
  public int getConcurrency() throws java.sql.SQLException {
    return rs.getConcurrency();
  }

  @Override
  public boolean rowUpdated() throws java.sql.SQLException {
    return rs.rowUpdated();
  }

  @Override
  public boolean rowInserted() throws java.sql.SQLException {
    return rs.rowInserted();
  }

  @Override
  public boolean rowDeleted() throws java.sql.SQLException {
    return rs.rowDeleted();
  }

  @Override
  public void updateNull(java.lang.String arg0) throws java.sql.SQLException {
    rs.updateNull(arg0);
  }

  @Override
  public void updateNull(int arg0) throws java.sql.SQLException {
    rs.updateNull(arg0);
  }

  @Override
  public void updateBoolean(int arg0,boolean arg1) throws java.sql.SQLException {
    rs.updateBoolean(arg0, arg1);
  }

  @Override
  public void updateBoolean(java.lang.String arg0,boolean arg1) throws java.sql.SQLException {
    rs.updateBoolean(arg0, arg1);
  }

  @Override
  public void updateByte(java.lang.String arg0,byte arg1) throws java.sql.SQLException {
    rs.updateByte(arg0, arg1);
  }

  @Override
  public void updateByte(int arg0,byte arg1) throws java.sql.SQLException {
    rs.updateByte(arg0, arg1);
  }

  @Override
  public void updateShort(int arg0,short arg1) throws java.sql.SQLException {
    rs.updateShort(arg0, arg1);
  }

  @Override
  public void updateShort(java.lang.String arg0,short arg1) throws java.sql.SQLException {
    rs.updateShort(arg0, arg1);
  }

  @Override
  public void updateInt(java.lang.String arg0,int arg1) throws java.sql.SQLException {
    rs.updateInt(arg0, arg1);
  }

  @Override
  public void updateInt(int arg0,int arg1) throws java.sql.SQLException {
    rs.updateInt(arg0, arg1);
  }

  @Override
  public void updateLong(java.lang.String arg0,long arg1) throws java.sql.SQLException {
    rs.updateLong(arg0, arg1);
  }

  @Override
  public void updateLong(int arg0,long arg1) throws java.sql.SQLException {
    rs.updateLong(arg0, arg1);
  }

  @Override
  public void updateFloat(int arg0,float arg1) throws java.sql.SQLException {
    rs.updateFloat(arg0, arg1);
  }

  @Override
  public void updateFloat(java.lang.String arg0,float arg1) throws java.sql.SQLException {
    rs.updateFloat(arg0, arg1);
  }

  @Override
  public void updateDouble(int arg0,double arg1) throws java.sql.SQLException {
    rs.updateDouble(arg0, arg1);
  }

  @Override
  public void updateDouble(java.lang.String arg0,double arg1) throws java.sql.SQLException {
    rs.updateDouble(arg0, arg1);
  }

  @Override
  public void updateBigDecimal(java.lang.String arg0,java.math.BigDecimal arg1) throws java.sql.SQLException {
    rs.updateBigDecimal(arg0, arg1);
  }

  @Override
  public void updateBigDecimal(int arg0,java.math.BigDecimal arg1) throws java.sql.SQLException {
    rs.updateBigDecimal(arg0, arg1);
  }

  @Override
  public void updateString(java.lang.String arg0,java.lang.String arg1) throws java.sql.SQLException {
    rs.updateString(arg0, arg1);
  }

  @Override
  public void updateString(int arg0,java.lang.String arg1) throws java.sql.SQLException {
    rs.updateString(arg0, arg1);
  }

  @Override
  public void updateDate(java.lang.String arg0,java.sql.Date arg1) throws java.sql.SQLException {
    rs.updateDate(arg0, arg1);
  }

  @Override
  public void updateDate(int arg0,java.sql.Date arg1) throws java.sql.SQLException {
    rs.updateDate(arg0, arg1);
  }

  @Override
  public void updateTime(int arg0,java.sql.Time arg1) throws java.sql.SQLException {
    rs.updateTime(arg0, arg1);
  }

  @Override
  public void updateTime(java.lang.String arg0,java.sql.Time arg1) throws java.sql.SQLException {
    rs.updateTime(arg0, arg1);
  }

  @Override
  public void updateTimestamp(java.lang.String arg0,java.sql.Timestamp arg1) throws java.sql.SQLException {
    rs.updateTimestamp(arg0, arg1);
  }

  @Override
  public void updateTimestamp(int arg0,java.sql.Timestamp arg1) throws java.sql.SQLException {
    rs.updateTimestamp(arg0, arg1);
  }

  @Override
  public void updateAsciiStream(int arg0,java.io.InputStream arg1,long arg2) throws java.sql.SQLException {
    rs.updateAsciiStream(arg0, arg1, arg2);
  }

  @Override
  public void updateAsciiStream(java.lang.String arg0,java.io.InputStream arg1,int arg2) throws java.sql.SQLException {
    rs.updateAsciiStream(arg0, arg1, arg2);
  }

  @Override
  public void updateAsciiStream(int arg0,java.io.InputStream arg1) throws java.sql.SQLException {
    rs.updateAsciiStream(arg0, arg1);
  }

  @Override
  public void updateAsciiStream(java.lang.String arg0,java.io.InputStream arg1,long arg2) throws java.sql.SQLException {
    rs.updateAsciiStream(arg0, arg1, arg2);
  }

  @Override
  public void updateAsciiStream(int arg0,java.io.InputStream arg1,int arg2) throws java.sql.SQLException {
    rs.updateAsciiStream(arg0, arg1, arg2);
  }

  @Override
  public void updateAsciiStream(java.lang.String arg0,java.io.InputStream arg1) throws java.sql.SQLException {
    rs.updateAsciiStream(arg0, arg1);
  }

  @Override
  public void updateBinaryStream(int arg0,java.io.InputStream arg1,long arg2) throws java.sql.SQLException {
    rs.updateBinaryStream(arg0, arg1, arg2);
  }

  @Override
  public void updateBinaryStream(int arg0,java.io.InputStream arg1) throws java.sql.SQLException {
    rs.updateBinaryStream(arg0, arg1);
  }

  @Override
  public void updateBinaryStream(int arg0,java.io.InputStream arg1,int arg2) throws java.sql.SQLException {
    rs.updateBinaryStream(arg0, arg1, arg2);
  }

  @Override
  public void updateBinaryStream(java.lang.String arg0,java.io.InputStream arg1,long arg2) throws java.sql.SQLException {
    rs.updateBinaryStream(arg0, arg1, arg2);
  }

  @Override
  public void updateBinaryStream(java.lang.String arg0,java.io.InputStream arg1,int arg2) throws java.sql.SQLException {
    rs.updateBinaryStream(arg0, arg1, arg2);
  }

  @Override
  public void updateBinaryStream(java.lang.String arg0,java.io.InputStream arg1) throws java.sql.SQLException {
    rs.updateBinaryStream(arg0, arg1);
  }

  @Override
  public void updateCharacterStream(java.lang.String arg0,java.io.Reader arg1,long arg2) throws java.sql.SQLException {
    rs.updateCharacterStream(arg0, arg1, arg2);
  }

  @Override
  public void updateCharacterStream(int arg0,java.io.Reader arg1) throws java.sql.SQLException {
    rs.updateCharacterStream(arg0, arg1);
  }

  @Override
  public void updateCharacterStream(java.lang.String arg0,java.io.Reader arg1) throws java.sql.SQLException {
    rs.updateCharacterStream(arg0, arg1);
  }

  @Override
  public void updateCharacterStream(int arg0,java.io.Reader arg1,int arg2) throws java.sql.SQLException {
    rs.updateCharacterStream(arg0, arg1, arg2);
  }

  @Override
  public void updateCharacterStream(int arg0,java.io.Reader arg1,long arg2) throws java.sql.SQLException {
    rs.updateCharacterStream(arg0, arg1, arg2);
  }

  @Override
  public void updateCharacterStream(java.lang.String arg0,java.io.Reader arg1,int arg2) throws java.sql.SQLException {
    rs.updateCharacterStream(arg0, arg1, arg2);
  }

  @Override
  public void updateObject(int arg0,java.lang.Object arg1,java.sql.SQLType arg2) throws java.sql.SQLException {
    rs.updateObject(arg0, arg1, arg2);
  }

  @Override
  public void updateObject(int arg0,java.lang.Object arg1,java.sql.SQLType arg2,int arg3) throws java.sql.SQLException {
    rs.updateObject(arg0, arg1, arg2, arg3);
  }

  @Override
  public void updateObject(java.lang.String arg0,java.lang.Object arg1,java.sql.SQLType arg2,int arg3) throws java.sql.SQLException {
    rs.updateObject(arg0, arg1, arg2, arg3);
  }

  @Override
  public void updateObject(int arg0,java.lang.Object arg1,int arg2) throws java.sql.SQLException {
    rs.updateObject(arg0, arg1, arg2);
  }

  @Override
  public void updateObject(java.lang.String arg0,java.lang.Object arg1,int arg2) throws java.sql.SQLException {
    rs.updateObject(arg0, arg1, arg2);
  }

  @Override
  public void updateObject(java.lang.String arg0,java.lang.Object arg1,java.sql.SQLType arg2) throws java.sql.SQLException {
    rs.updateObject(arg0, arg1, arg2);
  }

  @Override
  public void updateObject(int arg0,java.lang.Object arg1) throws java.sql.SQLException {
    rs.updateObject(arg0, arg1);
  }

  @Override
  public void updateObject(java.lang.String arg0,java.lang.Object arg1) throws java.sql.SQLException {
    rs.updateObject(arg0, arg1);
  }

  @Override
  public void insertRow() throws java.sql.SQLException {
    rs.insertRow();
  }

  @Override
  public void updateRow() throws java.sql.SQLException {
    rs.updateRow();
  }

  @Override
  public void deleteRow() throws java.sql.SQLException {
    rs.deleteRow();
  }

  @Override
  public void refreshRow() throws java.sql.SQLException {
    rs.refreshRow();
  }

  @Override
  public void cancelRowUpdates() throws java.sql.SQLException {
    rs.cancelRowUpdates();
  }

  @Override
  public void moveToInsertRow() throws java.sql.SQLException {
    rs.moveToInsertRow();
  }

  @Override
  public void moveToCurrentRow() throws java.sql.SQLException {
    rs.moveToCurrentRow();
  }

  @Override
  public java.sql.Statement getStatement() throws java.sql.SQLException {
    return rs.getStatement();
  }

  @Override
  public java.sql.Blob getBlob(java.lang.String arg0) throws java.sql.SQLException {
    return rs.getBlob(arg0);
  }

  @Override
  public java.sql.Blob getBlob(int arg0) throws java.sql.SQLException {
    return rs.getBlob(arg0);
  }

  @Override
  public java.sql.Clob getClob(int arg0) throws java.sql.SQLException {
    return rs.getClob(arg0);
  }

  @Override
  public java.sql.Clob getClob(java.lang.String arg0) throws java.sql.SQLException {
    return rs.getClob(arg0);
  }

  @Override
  public void updateRef(int arg0,java.sql.Ref arg1) throws java.sql.SQLException {
    rs.updateRef(arg0, arg1);
  }

  @Override
  public void updateRef(java.lang.String arg0,java.sql.Ref arg1) throws java.sql.SQLException {
    rs.updateRef(arg0, arg1);
  }

  @Override
  public void updateBlob(int arg0,java.io.InputStream arg1) throws java.sql.SQLException {
    rs.updateBlob(arg0, arg1);
  }

  @Override
  public void updateBlob(java.lang.String arg0,java.io.InputStream arg1) throws java.sql.SQLException {
    rs.updateBlob(arg0, arg1);
  }

  @Override
  public void updateBlob(int arg0,java.sql.Blob arg1) throws java.sql.SQLException {
    rs.updateBlob(arg0, arg1);
  }

  @Override
  public void updateBlob(int arg0,java.io.InputStream arg1,long arg2) throws java.sql.SQLException {
    rs.updateBlob(arg0, arg1, arg2);
  }

  @Override
  public void updateBlob(java.lang.String arg0,java.sql.Blob arg1) throws java.sql.SQLException {
    rs.updateBlob(arg0, arg1);
  }

  @Override
  public void updateBlob(java.lang.String arg0,java.io.InputStream arg1,long arg2) throws java.sql.SQLException {
    rs.updateBlob(arg0, arg1, arg2);
  }

  @Override
  public void updateClob(int arg0,java.io.Reader arg1,long arg2) throws java.sql.SQLException {
    rs.updateClob(arg0, arg1, arg2);
  }

  @Override
  public void updateClob(int arg0,java.io.Reader arg1) throws java.sql.SQLException {
    rs.updateClob(arg0, arg1);
  }

  @Override
  public void updateClob(java.lang.String arg0,java.sql.Clob arg1) throws java.sql.SQLException {
    rs.updateClob(arg0, arg1);
  }

  @Override
  public void updateClob(int arg0,java.sql.Clob arg1) throws java.sql.SQLException {
    rs.updateClob(arg0, arg1);
  }

  @Override
  public void updateClob(java.lang.String arg0,java.io.Reader arg1) throws java.sql.SQLException {
    rs.updateClob(arg0, arg1);
  }

  @Override
  public void updateClob(java.lang.String arg0,java.io.Reader arg1,long arg2) throws java.sql.SQLException {
    rs.updateClob(arg0, arg1, arg2);
  }

  @Override
  public void updateArray(int arg0,java.sql.Array arg1) throws java.sql.SQLException {
    rs.updateArray(arg0, arg1);
  }

  @Override
  public void updateArray(java.lang.String arg0,java.sql.Array arg1) throws java.sql.SQLException {
    rs.updateArray(arg0, arg1);
  }

  @Override
  public java.sql.RowId getRowId(java.lang.String arg0) throws java.sql.SQLException {
    return rs.getRowId(arg0);
  }

  @Override
  public java.sql.RowId getRowId(int arg0) throws java.sql.SQLException {
    return rs.getRowId(arg0);
  }

  @Override
  public void updateRowId(java.lang.String arg0,java.sql.RowId arg1) throws java.sql.SQLException {
    rs.updateRowId(arg0, arg1);
  }

  @Override
  public void updateRowId(int arg0,java.sql.RowId arg1) throws java.sql.SQLException {
    rs.updateRowId(arg0, arg1);
  }

  @Override
  public int getHoldability() throws java.sql.SQLException {
    return rs.getHoldability();
  }

  @Override
  public void updateNString(int arg0,java.lang.String arg1) throws java.sql.SQLException {
    rs.updateNString(arg0, arg1);
  }

  @Override
  public void updateNString(java.lang.String arg0,java.lang.String arg1) throws java.sql.SQLException {
    rs.updateNString(arg0, arg1);
  }

  @Override
  public void updateNClob(java.lang.String arg0,java.sql.NClob arg1) throws java.sql.SQLException {
    rs.updateNClob(arg0, arg1);
  }

  @Override
  public void updateNClob(int arg0,java.sql.NClob arg1) throws java.sql.SQLException {
    rs.updateNClob(arg0, arg1);
  }

  @Override
  public void updateNClob(java.lang.String arg0,java.io.Reader arg1) throws java.sql.SQLException {
    rs.updateNClob(arg0, arg1);
  }

  @Override
  public void updateNClob(int arg0,java.io.Reader arg1) throws java.sql.SQLException {
    rs.updateNClob(arg0, arg1);
  }

  @Override
  public void updateNClob(int arg0,java.io.Reader arg1,long arg2) throws java.sql.SQLException {
    rs.updateNClob(arg0, arg1, arg2);
  }

  @Override
  public void updateNClob(java.lang.String arg0,java.io.Reader arg1,long arg2) throws java.sql.SQLException {
    rs.updateNClob(arg0, arg1, arg2);
  }

  @Override
  public java.sql.NClob getNClob(java.lang.String arg0) throws java.sql.SQLException {
    return rs.getNClob(arg0);
  }

  @Override
  public java.sql.NClob getNClob(int arg0) throws java.sql.SQLException {
    return rs.getNClob(arg0);
  }

  @Override
  public java.sql.SQLXML getSQLXML(int arg0) throws java.sql.SQLException {
    return rs.getSQLXML(arg0);
  }

  @Override
  public java.sql.SQLXML getSQLXML(java.lang.String arg0) throws java.sql.SQLException {
    return rs.getSQLXML(arg0);
  }

  @Override
  public void updateSQLXML(java.lang.String arg0,java.sql.SQLXML arg1) throws java.sql.SQLException {
    rs.updateSQLXML(arg0, arg1);
  }

  @Override
  public void updateSQLXML(int arg0,java.sql.SQLXML arg1) throws java.sql.SQLException {
    rs.updateSQLXML(arg0, arg1);
  }

  @Override
  public java.lang.String getNString(int arg0) throws java.sql.SQLException {
    return rs.getNString(arg0);
  }

  @Override
  public java.lang.String getNString(java.lang.String arg0) throws java.sql.SQLException {
    return rs.getNString(arg0);
  }

  @Override
  public java.io.Reader getNCharacterStream(java.lang.String arg0) throws java.sql.SQLException {
    return rs.getNCharacterStream(arg0);
  }

  @Override
  public java.io.Reader getNCharacterStream(int arg0) throws java.sql.SQLException {
    return rs.getNCharacterStream(arg0);
  }

  @Override
  public void updateNCharacterStream(java.lang.String arg0,java.io.Reader arg1) throws java.sql.SQLException {
    rs.updateNCharacterStream(arg0, arg1);
  }

  @Override
  public void updateNCharacterStream(int arg0,java.io.Reader arg1,long arg2) throws java.sql.SQLException {
    rs.updateNCharacterStream(arg0, arg1, arg2);
  }

  @Override
  public void updateNCharacterStream(java.lang.String arg0,java.io.Reader arg1,long arg2) throws java.sql.SQLException {
    rs.updateNCharacterStream(arg0, arg1, arg2);
  }

  @Override
  public void updateNCharacterStream(int arg0,java.io.Reader arg1) throws java.sql.SQLException {
    rs.updateNCharacterStream(arg0, arg1);
  }

  @Override
  public boolean isClosed() throws java.sql.SQLException {
    return rs.isClosed();
  }

  @Override
  public java.sql.Time getTime(java.lang.String arg0) throws java.sql.SQLException {
    return rs.getTime(arg0);
  }

  @Override
  public java.sql.Time getTime(int arg0) throws java.sql.SQLException {
    return rs.getTime(arg0);
  }

  @Override
  public java.sql.Time getTime(java.lang.String arg0,java.util.Calendar arg1) throws java.sql.SQLException {
    return rs.getTime(arg0, arg1);
  }

  @Override
  public java.sql.Time getTime(int arg0,java.util.Calendar arg1) throws java.sql.SQLException {
    return rs.getTime(arg0, arg1);
  }

  @Override
  public boolean last() throws java.sql.SQLException {
    return rs.last();
  }

  @Override
  public java.sql.Date getDate(java.lang.String arg0,java.util.Calendar arg1) throws java.sql.SQLException {
    return rs.getDate(arg0, arg1);
  }

  @Override
  public java.sql.Date getDate(int arg0) throws java.sql.SQLException {
    return rs.getDate(arg0);
  }

  @Override
  public java.sql.Date getDate(java.lang.String arg0) throws java.sql.SQLException {
    return rs.getDate(arg0);
  }

  @Override
  public java.sql.Date getDate(int arg0,java.util.Calendar arg1) throws java.sql.SQLException {
    return rs.getDate(arg0, arg1);
  }

  @Override
  public java.sql.Timestamp getTimestamp(java.lang.String arg0) throws java.sql.SQLException {
    return rs.getTimestamp(arg0);
  }

  @Override
  public java.sql.Timestamp getTimestamp(int arg0,java.util.Calendar arg1) throws java.sql.SQLException {
    return rs.getTimestamp(arg0, arg1);
  }

  @Override
  public java.sql.Timestamp getTimestamp(int arg0) throws java.sql.SQLException {
    return rs.getTimestamp(arg0);
  }

  @Override
  public java.sql.Timestamp getTimestamp(java.lang.String arg0,java.util.Calendar arg1) throws java.sql.SQLException {
    return rs.getTimestamp(arg0, arg1);
  }

  @Override
  public java.lang.String getString(java.lang.String arg0) throws java.sql.SQLException {
    return rs.getString(arg0);
  }

  @Override
  public java.lang.String getString(int arg0) throws java.sql.SQLException {
    return rs.getString(arg0);
  }

  @Override
  public <T> T unwrap(java.lang.Class<T> arg0) throws java.sql.SQLException {
    return rs.unwrap(arg0);
  }

  @Override
  public boolean isWrapperFor(java.lang.Class<?> arg0) throws java.sql.SQLException {
    return rs.isWrapperFor(arg0);
  }

  public static void main(String[] args) throws Exception {

//    Method nextMethod = ResultSet.class.getMethod("next", null);
//    checkMethod("rs", ResultSet.class, nextMethod);
//
//    Method closeMethod = ResultSet.class.getMethod("close", null);
//    checkMethod("rs", ResultSet.class, closeMethod);
//
//    Method getBigDecimalMethod = ResultSet.class.getMethod("getBigDecimal", int.class, int.class);
//    checkMethod("rs", ResultSet.class, getBigDecimalMethod);

    Method[] methods = ResultSet.class.getMethods();
    for (Method method : methods) {
      if (!method.getName().equals("main")) {
        checkMethod("rs", ResultSet.class, method);
      }
    }

//    Method getNString = ResultSet.class.getMethod("getNString", int.class);
//    System.out.println(sharedToGenericString(getNString));
//    checkMethod("rs", ResultSet.class, getObject);

  }

  private static void checkMethod(String instanceName, Class clazz, Method method) {
    StringBuilder sb = new StringBuilder();
    sb.append("@Override").append("\n");
    sb.append(genOuterMethodPart(clazz, method));
    sb.append(" {").append("\n");
    sb.append(genInnerMethodPart(instanceName, method)).append("\n");
    sb.append("}").append("\n");
    System.out.println(sb.toString());
  }

  private static String genOuterMethodPart(Class clazz, Method method) {
    String methodString = sharedToGenericString(method)
            .replace(clazz.getName() + ".", "")
            .replace("abstract ", "")
            .replace("default ", "");
    for (Class interfaceClass : clazz.getInterfaces()) {
      methodString = methodString.replace(interfaceClass.getName() + ".", "");
    }
    return methodString;
  }

  private static String sharedToGenericString(Method method) {
    try {
      StringBuilder sb = new StringBuilder();

      int modifierMask = Modifier.methodModifiers();
      boolean isDefault = method.isDefault();

      int mod = method.getModifiers() & modifierMask;

      if (mod != 0 && !isDefault) {
        sb.append(Modifier.toString(mod)).append(' ');
      } else {
        int access_mod = mod & (Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE);
        if (access_mod != 0)
          sb.append(Modifier.toString(access_mod)).append(' ');
        if (isDefault)
          sb.append("default ");
        mod = (mod & ~(Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE));
        if (mod != 0)
          sb.append(Modifier.toString(mod)).append(' ');
      }

      TypeVariable<?>[] typeparms = method.getTypeParameters();
      if (typeparms.length > 0) {
        boolean first = true;
        sb.append('<');
        for(TypeVariable<?> typeparm: typeparms) {
          if (!first)
            sb.append(',');
          // Class objects can't occur here; no need to test
          // and call Class.getName().
          sb.append(typeparm.toString());
          first = false;
        }
        sb.append("> ");
      }

      Type genRetType = method.getGenericReturnType();
      sb.append(genRetType.getTypeName()).append(' ');
      sb.append(method.getDeclaringClass().getTypeName()).append('.');
      sb.append(method.getName());

      sb.append('(');
      Parameter[] parameters = method.getParameters();
      Type[] params = method.getGenericParameterTypes();
      for (int j = 0; j < params.length; j++) {
        String param = params[j].getTypeName();
        if (method.isVarArgs() && (j == params.length - 1)) // replace T[] with T...
          param = param.replaceFirst("\\[\\]$", "...");
        sb.append(param);
        sb.append(" " + parameters[j].getName());
        if (j < (params.length - 1))
          sb.append(',');
      }
      sb.append(')');
      Type[] exceptions = method.getGenericExceptionTypes();
      if (exceptions.length > 0) {
        sb.append(" throws ");
        for (int k = 0; k < exceptions.length; k++) {
          sb.append((exceptions[k] instanceof Class)?
                  ((Class)exceptions[k]).getName():
                  exceptions[k].toString());
          if (k < (exceptions.length - 1))
            sb.append(',');
        }
      }
      return sb.toString();
    } catch (Exception e) {
      return "<" + e + ">";
    }
  }

  private static String genInnerMethodPart(String instanceName, Method method) {
    Parameter[] parameters = method.getParameters();
    String someReturn = method.getReturnType().getName().equals("void")? "" : "return ";
    StringBuilder methodString = new StringBuilder(someReturn + instanceName + "." + method.getName() + "(");
    if (parameters != null && parameters.length > 0) {
      String args = Arrays.stream(parameters).map(Parameter::getName).collect(Collectors.joining(", "));
      methodString.append(args);
    }
    methodString.append(");");
    return methodString.toString();
  }
}
