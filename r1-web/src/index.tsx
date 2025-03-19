// @ts-ignore
import React, { useState } from 'react';
import { ConfigProvider } from 'antd';
// 由于 antd 组件的默认文案是英文，所以需要修改为中文
// @ts-ignore
import dayjs from 'dayjs';
import { createRoot } from 'react-dom/client';
import {BrowserRouter as Router, Routes, Route} from 'react-router-dom';

import 'dayjs/locale/zh-cn';

import zhCN from 'antd/locale/zh_CN';

import './index.css';
import Home from "./pages/home";

dayjs.locale('zh-cn');

const App = () => {
    // @ts-ignore
    return (
        <ConfigProvider locale={zhCN}>
            <Router>
                <Routes>
                    <Route path="/" element={<Home />} />
                </Routes>
            </Router>
        </ConfigProvider>
    );
};

// @ts-ignore
createRoot(document.getElementById('root')).render(<App />);