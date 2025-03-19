import React, {useState} from 'react';
import axios from 'axios';

interface PasswordModalProps {
    onClose: () => void; // 关闭弹窗的回调
    onSubmit: (password: string) => void; // 提交密码的回调
}

const PasswordModal: React.FC<PasswordModalProps> = ({onClose, onSubmit}) => {
    const [password, setPassword] = useState('');

    const handleSubmit = () => {
        if (!password) {
            alert("不能为空！")
            return;
        }
        onSubmit(password); // 提交密码
        onClose(); // 关闭弹窗
    };

    return (
        <div style={{
            position: 'fixed',
            top: '50%',
            left: '50%',
            transform: 'translate(-50%, -50%)',
            backgroundColor: 'white',
            padding: '20px'
        }}>
            <h3>请输入密码</h3>
            <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="系统启动时env参数password"
                style={{width: '100%', padding: '8px', marginBottom: '10px'}}
            />
            <button onClick={handleSubmit} style={{marginRight: '10px'}}>提交</button>
            <button onClick={onClose}>取消</button>
        </div>
    );
};

export default PasswordModal;