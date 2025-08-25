import React, {useEffect, useState} from "react";
import {Button, Col, Collapse, Form, FormInstance, Input, InputNumber, message, Row, Select, Space, Tabs} from "antd";
import {SaveOutlined} from "@ant-design/icons";
import {Device, R1Resources} from "../model/R1AdminData";

const {Option} = Select;
const {Panel} = Collapse;

interface DeviceFormProps {
    devices?: Device[];  // devices will be an array of Device objects
    handleSaveDevice: (device: Device) => void;
    r1Resources: R1Resources;
    formInstance: FormInstance<Device>;
    initValues?: Device;
}

const DeviceForm: React.FC<DeviceFormProps> = ({handleSaveDevice, initValues, r1Resources, formInstance}) => {

    const [musicChoice, setMusicChoice] = useState<string | undefined>("");
    const [musicEndpoint, setMusicEndpoint] = useState<string | undefined>("");
    const [aiChoice, setAiChoice] = useState<string | undefined>("");

    useEffect(() => {
        console.log("ini", initValues)
        setMusicChoice(initValues?.musicConfig?.choice)
        setMusicEndpoint(initValues?.musicConfig?.endpoint)
        setAiChoice(initValues?.aiConfig?.choice)
    }, [initValues]);

    const handleMusicSourceChange = (value: string) => {
        setMusicChoice(value);
    }

    const handleEndpointChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setMusicEndpoint(e.target.value);
    }

    const handleAiChange = (value: string) => {
        setAiChoice(value);
    }

    return (

        <Form form={formInstance} preserve initialValues={initValues} onFinish={handleSaveDevice} layout="vertical">
            <Form.Item name="id" label="设备 ID" rules={[{required: true}]}>
                <Input disabled className="form-input" placeholder={"劫持后，请喊一遍小讯小讯，重新刷新页面，自动填充"}/>
            </Form.Item>
            <Form.Item name="name" label="设备名称" rules={[{required: true}]}>
                <Input className="form-input"/>
            </Form.Item>

            <Collapse defaultActiveKey={["1"]} className="form-input" destroyInactivePanel={false}>
                <Panel header="AI 配置" key="1" forceRender>
                    <Form.Item name={["aiConfig", "choice"]} label="AI 选择">
                        <Select className="form-input" onChange={(value) => handleAiChange(value)}>
                            {r1Resources.aiList.map(item => {
                                return <Option key={item.serviceName} value={item.serviceName}>{item.aliasName}</Option>
                            })}
                        </Select>
                    </Form.Item>

                    {(aiChoice === 'OpenAi' || aiChoice === 'Gemini' ) && <>

                        <Form.Item name={["aiConfig", "endpoint"]} label="endpoint(不包含/chat/completions部分)">
                            <Input className="form-input"/>
                        </Form.Item>

                        <Form.Item name={["aiConfig", "model"]} label="模型名称">
                            <Input className="form-input"/>
                        </Form.Item>

                    </>}

                    <Form.Item name={["aiConfig", "key"]} label="AI Key">
                        <Input className="form-input"/>
                    </Form.Item>
                    <Form.Item name={["aiConfig", "systemPrompt"]} label="AI系统提示词">
                        <Input className="form-input" placeholder={"你是一个智能音箱"}/>
                    </Form.Item>
                    <Form.Item name={["aiConfig", "chatHistoryNum"]} label="AI聊天上下文（失效，会让小讯变傻）">
                        <InputNumber className="form-input"/>
                    </Form.Item>



                </Panel>

                <Panel header="HASS 配置" key="2" forceRender>
                    <Form.Item name={["hassConfig", "endpoint"]} label="HASS 地址">
                        <Input className="form-input"/>
                    </Form.Item>
                    <Form.Item name={["hassConfig", "token"]} label="HASS Token">
                        <Input className="form-input"/>
                    </Form.Item>
                </Panel>

                <Panel header="音乐配置" key="3" forceRender>
                    <Form.Item name={["musicConfig", "choice"]} label="音乐源选择">
                        <Select className="form-input" onChange={(value) => handleMusicSourceChange(value)}>
                            {r1Resources.musicList.map(item => {
                                return <Option key={item.serviceName} value={item.serviceName}>{item.aliasName}</Option>
                            })}
                        </Select>
                    </Form.Item>

                    <Row> <Col span={18}>
                        {
                            musicChoice !== "gequbao" &&
                            <Form.Item name={["musicConfig", "endpoint"]} label="音乐源接口">
                                <Input className="form-input" onChange={(value) => handleEndpointChange(value)}/>
                            </Form.Item>
                        }
                    </Col>
                        <Col>
                            {
                                musicChoice === "NetEaseMusic" && <>
                                    <Button style={{"marginTop": "30px", "paddingRight": "15px"}} type={"link"}
                                            href={`${musicEndpoint}/qrlogin.html`} target={"_blank"}>点我二维码登录</Button>
                                </>
                            }

                        </Col>
                    </Row>
                </Panel>

                <Panel header="有声读物" key="4" forceRender>
                    <Form.Item name={["audioConfig", "choice"]} label="音频源选择">
                        <Select className="form-input">
                            {r1Resources.audioList.map(item => {
                                return <Option key={item.serviceName} value={item.serviceName}>{item.aliasName}</Option>
                            })}
                        </Select>
                    </Form.Item>
                </Panel>

                <Panel header="天气配置" key="5" forceRender>
                    <Form.Item name={["weatherConfig", "choice"]} label="天气源">
                        <Select className="form-input">
                            {r1Resources.weatherList.map(item => {
                                return <Option key={item.serviceName} value={item.serviceName}>{item.aliasName}</Option>
                            })}
                        </Select>
                    </Form.Item>
                    <Form.Item name={["weatherConfig", "endpoint"]} label="天气API地址">
                        <Input className="form-input" placeholder={"每个人不一样"}/>
                    </Form.Item>
                    <Form.Item name={["weatherConfig", "key"]} label="天气API KEY">
                        <Input className="form-input" placeholder={"不是私钥"}/>
                    </Form.Item>
                    <Form.Item name={["weatherConfig", "locationId"]} label="默认城市">
                        <Select
                            showSearch
                            placeholder="请选择城市"
                            optionFilterProp="children"
                            filterOption={(input: string, option?: { children: string }) =>
                                (option?.children ?? '').toLowerCase().includes(input.toLowerCase())
                            }
                        >
                            {r1Resources.cityLocations.map(city => (
                                <Select.Option
                                    key={city.locationId}
                                    value={city.locationId}
                                >
                                    {city.cityName}
                                </Select.Option>
                            ))}
                        </Select>
                    </Form.Item>
                </Panel>

                <Panel header="新闻配置" key="6" forceRender>
                    <Form.Item name={["newsConfig", "choice"]} label="新闻源选择">
                        <Select className="form-input">
                            {r1Resources.newsList.map(item => {
                                return <Option key={item.serviceName} value={item.serviceName}>{item.aliasName}</Option>
                            })}
                        </Select>
                    </Form.Item>
                </Panel>
            </Collapse>

            <Form.Item>
                <Button type="primary" htmlType="submit" icon={<SaveOutlined/>} className="button-save">
                    保存配置
                </Button>
            </Form.Item>
        </Form>
    )
}

export default DeviceForm;

