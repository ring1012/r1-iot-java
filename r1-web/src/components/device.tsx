import React, {useEffect} from "react";
import {Button, Collapse, Form, FormInstance, Input, message, Select, Tabs} from "antd";
import {SaveOutlined} from "@ant-design/icons";
import {Device, R1Resources} from "../model/R1AdminData";

const { Option } = Select;
const { Panel } = Collapse;
interface DeviceFormProps {
    devices?: Device[];  // devices will be an array of Device objects
    handleSaveDevice: (device: Device) => void;
    r1Resources: R1Resources;
    formInstance: FormInstance<Device>;
    initValues?: Device;
}

const DeviceForm: React.FC<DeviceFormProps> = ({handleSaveDevice, initValues, r1Resources, formInstance}) => {

    useEffect(() => {
        console.log("ini", initValues)
    }, [initValues]);

    return (

        <Form form={formInstance} initialValues={initValues} onFinish={handleSaveDevice} layout="vertical">
            <Form.Item name="id" label="设备 ID" rules={[{ required: true }]}>
                <Input disabled className="form-input" placeholder={"劫持后，请喊一遍小讯小讯，重新刷新页面，自动填充"}/>
            </Form.Item>
            <Form.Item name="name" label="设备名称" rules={[{ required: true }]}>
                <Input className="form-input"  />
            </Form.Item>

            <Collapse defaultActiveKey={['1']} className="form-input">
                <Panel header="AI 配置" key="1">
                    <Form.Item name={["aiConfig", "choice"]} label="AI 选择" >
                        <Select className="form-input">
                            {r1Resources.aiList.map(item =>{
                                return <Option key={item.serviceName} value={item.serviceName}>{item.aliasName}</Option>
                            })}
                        </Select>
                    </Form.Item>
                    <Form.Item name={["aiConfig", "key"]} label="AI Key" >
                        <Input className="form-input" />
                    </Form.Item>
                </Panel>

                <Panel header="HASS 配置" key="2">
                    <Form.Item name={["hassConfig", "endpoint"]} label="HASS 地址" >
                        <Input className="form-input" />
                    </Form.Item>
                    <Form.Item name={["hassConfig", "token"]} label="HASS Token" >
                        <Input className="form-input" />
                    </Form.Item>
                </Panel>

                <Panel header="新闻配置" key="3">
                    <Form.Item name={["newsConfig", "choice"]} label="新闻源选择" >
                        <Select className="form-input">
                            <Select className="form-input">
                                {r1Resources.audioList.map(item =>{
                                    return <Option key={item.serviceName} value={item.serviceName}>{item.aliasName}</Option>
                                })}
                            </Select>
                        </Select>
                    </Form.Item>
                </Panel>
            </Collapse>

            <Form.Item>
                <Button type="primary" htmlType="submit" icon={<SaveOutlined />} className="button-save">
                    保存配置
                </Button>
            </Form.Item>
        </Form>
    )
}

export default DeviceForm;
