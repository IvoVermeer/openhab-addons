/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.persistence.jdbc.db;

import org.knowm.yank.Yank;
import org.openhab.core.items.Item;
import org.openhab.persistence.jdbc.model.ItemVO;
import org.openhab.persistence.jdbc.model.ItemsVO;
import org.openhab.persistence.jdbc.utils.StringUtilsExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extended Database Configuration class. Class represents
 * the extended database-specific configuration. Overrides and supplements the
 * default settings from JdbcBaseDAO. Enter only the differences to JdbcBaseDAO here.
 *
 * @author Helmut Lehmeyer - Initial contribution
 */
public class JdbcHsqldbDAO extends JdbcBaseDAO {
    private static final Logger logger = LoggerFactory.getLogger(JdbcHsqldbDAO.class);

    /********
     * INIT *
     ********/
    public JdbcHsqldbDAO() {
        super();
        initSqlQueries();
        initSqlTypes();
        initDbProps();
    }

    private void initSqlQueries() {
        logger.debug("JDBC::initSqlQueries: '{}'", this.getClass().getSimpleName());
        // http://hsqldb.org/doc/guide/builtinfunctions-chapt.html
        SQL_PING_DB = "SELECT 1 FROM INFORMATION_SCHEMA.SYSTEM_USERS";
        SQL_GET_DB = "SELECT DATABASE () FROM INFORMATION_SCHEMA.SYSTEM_USERS";
        SQL_IF_TABLE_EXISTS = "SELECT * FROM INFORMATION_SCHEMA.SYSTEM_TABLES WHERE TABLE_NAME='#searchTable#'";
        SQL_CREATE_ITEMS_TABLE_IF_NOT = "CREATE TABLE IF NOT EXISTS #itemsManageTable# ( ItemId INT GENERATED BY DEFAULT AS IDENTITY (START WITH 1, INCREMENT BY 1) NOT NULL, #colname# #coltype# NOT NULL)";
        SQL_CREATE_NEW_ENTRY_IN_ITEMS_TABLE = "INSERT INTO #itemsManageTable# (ItemName) VALUES ('#itemname#')";
        // Prevent error against duplicate time value
        // http://hsqldb.org/doc/guide/dataaccess-chapt.html#dac_merge_statement
        // SQL_INSERT_ITEM_VALUE = "INSERT INTO #tableName# (TIME, VALUE) VALUES( NOW(), CAST( ? as #dbType#) )";
        SQL_INSERT_ITEM_VALUE = "MERGE INTO #tableName# "
                + "USING (VALUES #tablePrimaryValue#, CAST( ? as #dbType#)) temp (TIME, VALUE) ON (#tableName#.TIME=temp.TIME) "
                + "WHEN NOT MATCHED THEN INSERT (TIME, VALUE) VALUES (temp.TIME, temp.VALUE)";
    }

    /**
     * INFO: http://www.java2s.com/Code/Java/Database-SQL-JDBC/StandardSQLDataTypeswithTheirJavaEquivalents.htm
     */
    private void initSqlTypes() {
    }

    /**
     * INFO: https://github.com/brettwooldridge/HikariCP
     */
    private void initDbProps() {

        // Properties for HikariCP
        databaseProps.setProperty("driverClassName", "org.hsqldb.jdbcDriver");
    }

    /**************
     * ITEMS DAOs *
     **************/
    @Override
    public Integer doPingDB() {
        return Yank.queryScalar(SQL_PING_DB, Integer.class, null);
    }

    @Override
    public ItemsVO doCreateItemsTableIfNot(ItemsVO vo) {
        String sql = StringUtilsExt.replaceArrayMerge(SQL_CREATE_ITEMS_TABLE_IF_NOT,
                new String[] { "#itemsManageTable#", "#colname#", "#coltype#", "#itemsManageTable#" },
                new String[] { vo.getItemsManageTable(), vo.getColname(), vo.getColtype(), vo.getItemsManageTable() });
        logger.debug("JDBC::doCreateItemsTableIfNot sql={}", sql);
        Yank.execute(sql, null);
        return vo;
    }

    @Override
    public Long doCreateNewEntryInItemsTable(ItemsVO vo) {
        String sql = StringUtilsExt.replaceArrayMerge(SQL_CREATE_NEW_ENTRY_IN_ITEMS_TABLE,
                new String[] { "#itemsManageTable#", "#itemname#" },
                new String[] { vo.getItemsManageTable(), vo.getItemname() });
        logger.debug("JDBC::doCreateNewEntryInItemsTable sql={}", sql);
        return Yank.insert(sql, null);
    }

    /*************
     * ITEM DAOs *
     *************/
    @Override
    public void doStoreItemValue(Item item, ItemVO vo) {
        vo = storeItemValueProvider(item, vo);
        String sql = StringUtilsExt.replaceArrayMerge(SQL_INSERT_ITEM_VALUE,
                new String[] { "#tableName#", "#dbType#", "#tableName#", "#tablePrimaryValue#" }, new String[] {
                        vo.getTableName(), vo.getDbType(), vo.getTableName(), sqlTypes.get("tablePrimaryValue") });
        Object[] params = new Object[] { vo.getValue() };
        logger.debug("JDBC::doStoreItemValue sql={} value='{}'", sql, vo.getValue());
        Yank.execute(sql, params);
    }

    /****************************
     * SQL generation Providers *
     ****************************/

    /*****************
     * H E L P E R S *
     *****************/

    /******************************
     * public Getters and Setters *
     ******************************/

}
