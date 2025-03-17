import React from 'react';
import { Form, Input, Button, Select } from 'antd';
import {R1AdminData} from "../model/R1AdminData";

const { Option } = Select;

const Home = () => {
    const [form] = Form.useForm<R1AdminData>();

    const onFinish = (values: R1AdminData) => {
        console.log('Received values of form:', values);
    };

    return (
        <div style={{
            backgroundColor: '#f0f2f5',
            padding: '24px',
            minHeight: '100vh',
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center'
        }}>
            <Form
                form={form}
                onFinish={onFinish}
                layout="vertical"
                style={{
                    backgroundColor: '#fff',
                    padding: '24px',
                    borderRadius: '8px',
                    boxShadow: '0 2px 8px rgba(0, 0, 0, 0.1)',
                    width: '400px'
                }}
            >
                {/* Default AI */}
                <Form.Item<R1AdminData>
                    label="Default AI"
                    name="defaultAI" // 直接使用字段名
                    rules={[{ required: true, message: 'Please select the default AI!' }]}
                >
                    <Select placeholder="Select default AI">
                        <Option value="Gemini">Gemini</Option>
                        <Option value="Grok">Grok</Option>
                    </Select>
                </Form.Item>

                {/* System Info */}
                <Form.Item<R1AdminData>
                    label="System Info"
                    name={['chat', 'systemInfo']} // 嵌套字段使用数组语法
                    rules={[{ required: true, message: 'Please input the system info!' }]}
                >
                    <Input placeholder="Enter system info" />
                </Form.Item>

                {/* Chat AI */}
                <Form.Item<R1AdminData>
                    label="Chat AI"
                    name={['chat', 'chatAI']} // 嵌套字段使用数组语法
                    rules={[{ required: true, message: 'Please select the chat AI!' }]}
                >
                    <Select placeholder="Select chat AI">
                        <Option value="Gemini">Gemini</Option>
                        <Option value="Grok">Grok</Option>
                    </Select>
                </Form.Item>

                {/* Music */}
                <Form.Item<R1AdminData>
                    label="Music"
                    name="music" // 直接使用字段名
                    rules={[{ required: true, message: 'Please select the music platform!' }]}
                >
                    <Select placeholder="Select music platform">
                        <Option value="qq">QQ Music</Option>
                        <Option value="neast">Neast</Option>
                    </Select>
                </Form.Item>

                {/* Submit Button */}
                <Form.Item>
                    <Button type="primary" htmlType="submit" style={{ width: '100%' }}>
                        Submit
                    </Button>
                </Form.Item>
            </Form>
        </div>
  );
}


export default Home;
