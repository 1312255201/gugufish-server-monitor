// 导入ECharts库
import * as echarts from "echarts";

/**
 * 创建默认的ECharts图表配置
 * @param {string} name - Y轴名称
 * @param {Array} dataX - X轴数据数组
 * @return {Object} 返回基础配置对象
 */
function defaultOption(name, dataX) {
    return {
        // 提示框配置
        tooltip: {
            trigger: 'axis', // 触发类型，坐标轴触发
            position: function (pt) {
                return [pt[0], pt[1]]; // 提示框位置跟随鼠标
            },
            confine: true, // 将提示框限制在图表区域内
            padding: 3, // 提示框内边距
            backgroundColor: '#FFFFFFE0', // 提示框背景色（带透明度）
            textStyle: {
                fontSize: 13 // 提示框文字大小
            }
        }, 
        // 图表网格配置
        grid:{
            left: '10', // 左边距
            right: '15', // 右边距
            bottom: '0', // 下边距
            top: '30', // 上边距
            containLabel: true // 网格区域是否包含坐标轴的刻度标签
        }, 
        // X轴配置
        xAxis: {
            type: 'category', // 类目轴
            boundaryGap: false, // 坐标轴两边不留白
            data: dataX, // X轴数据
            animation: false, // 不开启动画
            axisLabel: {
                formatter: function (value) {
                    // 格式化X轴标签，显示时间和日期
                    value = new Date(value)
                    let time = value.toLocaleTimeString();
                    time = time.substring(0, time.length - 3) // 去掉秒
                    const date = [value.getDate() + 1, value.getMonth() + 1].join('/')
                    return `${time}\n${date}`; // 时间和日期分两行显示
                }
            }
        }, 
        // Y轴配置
        yAxis: {
            type: 'value', // 数值轴
            name: name, // Y轴名称
            boundaryGap: [0, '10%'] // Y轴两边留白比例
        }, 
        // 区域缩放组件配置
        dataZoom: [
            {
                type: 'inside', // 内置型数据区域缩放组件（使用鼠标滚轮缩放）
                start: 95, // 数据窗口范围的起始百分比
                end: 100, // 数据窗口范围的结束百分比
                minValueSpan: 12 // 最小缩放span
            }
        ]
    };
}

/**
 * 为图表添加单个数据系列
 * @param {Object} option - 图表配置对象
 * @param {string} name - 系列名称
 * @param {Array} dataY - Y轴数据数组
 * @param {Array} colors - 颜色数组，包含[线条颜色, 渐变起始色, 渐变结束色]
 */
function singleSeries(option, name, dataY, colors) {
    option.series = [
        {
            name: name, // 系列名称
            type: 'line', // 系列类型，线图
            sampling: 'lttb', // 采样方式，使用LTTB(Largest-Triangle-Three-Buckets)算法，在大数据量时提高渲染性能
            showSymbol: false, // 不显示标记点
            itemStyle: {
                color: colors[0] // 线条颜色
            },
            // 区域填充样式配置
            areaStyle: {
                // 创建线性渐变，从上到下
                color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                    {
                        offset: 0, // 渐变起点
                        color: colors[1] // 渐变起始颜色
                    }, {
                        offset: 1, // 渐变终点
                        color: colors[2] // 渐变结束颜色
                    }
                ])
            },
            data: dataY // Y轴数据
        }
    ]
}

/**
 * 为图表添加双数据系列
 * @param {Object} option - 图表配置对象
 * @param {Array} name - 系列名称数组，包含两个系列的名称
 * @param {Array} dataY - Y轴数据二维数组，包含两个系列的数据
 * @param {Array} colors - 颜色二维数组，每个系列包含[线条颜色, 渐变起始色, 渐变结束色]
 */
function doubleSeries(option, name, dataY, colors) {
    option.series = [
        {
            name: name[0], // 第一个系列的名称
            type: 'line', // 系列类型，线图
            sampling: 'lttb', // 采样方式，提高大数据量渲染性能
            showSymbol: false, // 不显示标记点
            itemStyle: {
                color: colors[0][0] // 第一个系列的线条颜色
            },
            // 第一个系列的区域填充样式
            areaStyle: {
                // 创建线性渐变
                color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                    {
                        offset: 0, // 渐变起点
                        color: colors[0][1] // 渐变起始颜色
                    }, {
                        offset: 1, // 渐变终点
                        color: colors[0][2] // 渐变结束颜色
                    }
                ])
            },
            data: dataY[0] // 第一个系列的Y轴数据
        }, {
            name: name[1], // 第二个系列的名称
            type: 'line', // 系列类型，线图
            sampling: 'lttb', // 采样方式
            showSymbol: false, // 不显示标记点
            // 第二个系列的线条样式
            itemStyle: {
                color: colors[1][0] // 第二个系列的线条颜色
            },
            // 第二个系列的区域填充样式
            areaStyle: {
                // 创建线性渐变
                color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                    {
                        offset: 0, // 渐变起点
                        color: colors[1][1] // 渐变起始颜色
                    }, {
                        offset: 1, // 渐变终点
                        color: colors[1][2] // 渐变结束颜色
                    }
                ])
            },
            data: dataY[1] // 第二个系列的Y轴数据
        }
    ]
}

// 导出函数供其他模块使用
export { defaultOption, singleSeries, doubleSeries }
