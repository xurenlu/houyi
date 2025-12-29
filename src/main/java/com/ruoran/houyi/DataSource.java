/*
 * This file is part of the zyan/wework-msgaudit.
 *
 * (c) 读心印 <aa24615@qq.com>
 *
 * This source file is subject to the MIT license that is bundled
 * with this source code in the file LICENSE.
 */

package com.ruoran.houyi;

import org.springframework.jdbc.datasource.DriverManagerDataSource;

public class DataSource {
    public static DriverManagerDataSource init(){

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://192.168.50.34:3306/message?useSSL=false&characterEncoding=utf-8&autoReconnect=true");
        dataSource.setUsername("msg");
        dataSource.setPassword("msg");

        return dataSource;
    }
}
