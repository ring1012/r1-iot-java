import React, {useEffect, useState} from "react";
import PasswordModal from "../../components/pop_pass";
import {Button, Col, Form, Input, message, Row} from "antd";
import axiosInstance from "../../components/api";
import {R1AdminData, R1GlobalConfig} from "../../model/R1AdminData";
import {AxiosError} from "axios";

const Server: React.FC = () => {
    const [showPasswordModal, setShowPasswordModal] = useState(false); // 控制弹窗显示
    const [r1AdminData, setR1AdminData] = useState<R1AdminData | null>(null);

    const apiURL = process.env.REACT_APP_API_URL;

    const [form] = Form.useForm();

    useEffect(() => {
        const fetchData = async () => {
            try {
                const response = await axiosInstance.get<R1AdminData>(`${apiURL}admin/resources`);
                const respData = response.data;
                setR1AdminData(respData);

                console.log("r1AdminData", respData)

                form.setFieldsValue(respData.r1GlobalConfig);
            } catch (err) {
                const error = err as AxiosError; // 类型断言为 AxiosError
                if (error.response && error.response.status === 403) {
                    setShowPasswordModal(true);
                } else {
                    console.error('Error:', error.message);
                }
            }
        };

        fetchData();
    }, [])

    const onFinish = async (values: R1GlobalConfig) => {

        try {
            await axiosInstance.post(`${apiURL}admin/globalConfig`, values)
            message.success("服务配置已更新！", 2);

        } catch (err) {
            const error = err as AxiosError; // 类型断言为 AxiosError
            if (error.response && error.response.status === 403) {
                setShowPasswordModal(true);
            } else {
                console.error('Error:', error.message);
            }
        }
    }

    return (
        <>
            {showPasswordModal && (
                <PasswordModal
                    onClose={() => setShowPasswordModal(false)}
                />
            )}

            <div className="container">

                <div className="card-container">

                    {r1AdminData &&

                        <Row gutter={[16, 16]}>
                            <Col span={24}>
                                <Form
                                    style={{"padding":"10px"}}
                                    form={form}
                                    layout="vertical"
                                    onFinish={onFinish}
                                    initialValues={r1AdminData.r1GlobalConfig}
                                >
                                    <Form.Item
                                        label="主机IP"
                                        name="hostIp"
                                        rules={[{message: '请输入主机IP'}]}
                                    >
                                        <Input placeholder="例如: 192.168.1.100:8080，确保R1音箱能访问到这个地址"/>
                                    </Form.Item>

                                    <Form.Item
                                        label="yt-dlp端点"
                                        name="ytdlpEndpoint"
                                        rules={[{message: '请输入yt-dlp端点'}]}
                                    >
                                        <Input placeholder="例如: http://example.com:5000，arm机器执行ty-dlp较慢"/>
                                    </Form.Item>

                                    <Form.Item
                                        label="Cloudflare服务ID"
                                        name="cfServiceId"
                                        rules={[{message: '请输入Cloudflare服务ID'}]}
                                    >
                                        <Input placeholder="例如: service-123，提供内网穿透"/>
                                    </Form.Item>

                                    <Form.Item>
                                        <div style={{display: 'flex', justifyContent: 'flex-end', gap: '12px'}}>

                                            <Button type="primary" htmlType="submit">
                                                保存配置
                                            </Button>
                                        </div>
                                    </Form.Item>
                                </Form>
                            </Col>
                        </Row>


                    }


                </div>
            </div>
        </>


    );

}

export default Server;