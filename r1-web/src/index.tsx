// @ts-ignore
import React, {useState} from 'react';
import {ConfigProvider, Menu, Layout, Button} from 'antd';
// 由于 antd 组件的默认文案是英文，所以需要修改为中文
// @ts-ignore
import dayjs from 'dayjs';
import {createRoot} from 'react-dom/client';
import {BrowserRouter as Router, Routes, Route, Link, useLocation} from 'react-router-dom';

import 'dayjs/locale/zh-cn';

import zhCN from 'antd/locale/zh_CN';

import './index.css';
import Box from "./pages/home";
import Server from "./pages/server";
import {CloudOutlined, SoundOutlined, MenuUnfoldOutlined, MenuFoldOutlined} from "@ant-design/icons";

const {Sider} = Layout;

dayjs.locale('zh-cn');

// 菜单组件
const VerticalMenu = () => {
    const location = useLocation();

    return (
        <Menu
            mode="inline"
            selectedKeys={[location.pathname === '/server' ? 'server' : 'box']}
            style={{height: '100%', borderRight: 0}}
        >
            <Menu.Item
                key="box"
                icon={<SoundOutlined/>}
            >
                <Link to="/">音箱配置</Link>
            </Menu.Item>

            <Menu.Item
                key="server"
                icon={<CloudOutlined/>}
            >
                <Link to="/server">服务器配置</Link>
            </Menu.Item>
        </Menu>
    );
};

const App = () => {

    const [collapsed, setCollapsed] = useState(true);

    // @ts-ignore
    return (
        <ConfigProvider locale={zhCN}>
            <Router>
                <Layout style={{minHeight: '100vh'}}>
                    <Sider
                        width={200}
                        theme="light"
                        collapsible
                        collapsed={collapsed}
                        onCollapse={(value) => setCollapsed(value)}
                        trigger={null} // 隐藏默认的折叠触发器
                    >
                        <div style={{padding: '16px', textAlign: 'center'}}>
                            <Button
                                type="text"
                                icon={collapsed ? <MenuUnfoldOutlined/> : <MenuFoldOutlined/>}
                                onClick={() => setCollapsed(!collapsed)}
                                style={{fontSize: '16px', width: '100%'}}
                            />
                        </div>
                        <VerticalMenu/>
                    </Sider>
                    <Layout>
                        <Routes>
                            <Route path="/" element={<Box/>}/>
                            <Route path="/server" element={<Server/>}/>
                        </Routes>
                    </Layout>
                </Layout>
            </Router>
        </ConfigProvider>
    );
};

// @ts-ignore
createRoot(document.getElementById('root')).render(<App/>);