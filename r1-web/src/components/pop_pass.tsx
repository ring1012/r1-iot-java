import React, {useState, useEffect} from 'react';
import {Button, Input, Modal} from 'antd';
import type {InputRef} from 'antd';
import axiosInstance from "./api";

const PasswordModal = ({onClose}: { onClose: () => void }) => {
    const [password, setPassword] = useState('');
    const inputRef = React.useRef<InputRef>(null);

    // 聚焦输入框
    useEffect(() => {
        inputRef.current?.focus();
    }, []);
    const apiURL = process.env.REACT_APP_API_URL;

    // 处理提交
    const handleSubmit = async () => {
        if (!password) {
            Modal.warning({
                title: '提示',
                content: '请输入密码',
            });
            return;
        }


        try {
            const response = await axiosInstance.post(`${apiURL}auth`, {password});
            if (response.status === 200) {
                window.localStorage.setItem("token", response.data);
                Modal.success({
                    title: '成功',
                    content: '密码验证成功',
                });
                // 刷新当前页面
                window.location.reload();
            }
        } catch (err) {
            console.error('验证失败:', err);

            Modal.error({
                title: '错误',
                content: '密码验证失败',
            });
        }

        onClose();

    };

    // 处理键盘事件
    const handleKeyDown = (e: React.KeyboardEvent) => {
        if (e.key === 'Enter') {
            handleSubmit();
        }
    };

    return (
        <div style={{
            position: 'fixed',
            top: '50%',
            left: '50%',
            transform: 'translate(-50%, -50%)',
            backgroundColor: 'white',
            padding: '20px',
            borderRadius: '8px',
            boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)',
            zIndex: 1000,
            width: '300px'
        }}>
            <h3 style={{marginBottom: '16px'}}>请输入密码</h3>
            <Input.Password
                ref={inputRef}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="系统启动时env参数password"
                style={{width: '100%', marginBottom: '16px'}}
                onKeyDown={handleKeyDown}
            />
            <div style={{display: 'flex', justifyContent: 'flex-end'}}>
                <Button
                    onClick={onClose}
                    style={{marginRight: '10px'}}
                >
                    取消
                </Button>
                <Button
                    type="primary"
                    onClick={handleSubmit}
                    loading={false} // 可以设置为true来显示加载状态
                >
                    提交
                </Button>
            </div>
        </div>
    );
};

export default PasswordModal;