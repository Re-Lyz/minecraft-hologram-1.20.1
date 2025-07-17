# Hologram_SJMC.ver
<p align="left">
  语言: <b>简体中文</b> |
  <a href="./README_EN.md">English</a>
</p>

## 简介

一个利用展示实体渲染模型的插件。

Youtube的演示视频:
1. [Working Globe Hologram in Minecraft](https://youtu.be/ae_Gns9ZBqY)
2. [3D Meshes with Text Displays in Minecraft](https://youtu.be/RnLWLQsh9mw)

该插件仅处于试验阶段，请注意使用风险。旧版使用方式与功能详见[英文文档](./README_EN.md)


## 安装方式
1. 在 [releases page](https://github.com/TheCymaera/minecraft-hologram/releases/) **[TODO]** 下载 jar 文件，需要安装worldedit或者类似插件作为前置（例如FAWE）。
2. 在[Paper](https://papermc.io/downloads) 或 [Spigot](https://getbukkit.org/download/spigot) 端或者相似服务端上安装插件（本插件的测试环境为 Purpur 1.20.1）。


## 指令
此处主要介绍新添加的指令，旧版指令使用方式详见[英文文档](./README_EN.md)

### Model

#### Help

```
/model help
```

游戏内查看指令使用方式。

#### Add

```
/model add <模型名>
```

将需要渲染的模型的.obj文件与相关贴图.png文件（可选）整合在一个文件夹中，所有文件及文件夹保持同名称一并放到models文件夹下，使用指令加载模型。

#### list

```
/model list
```

列举已经加载的模型列表。

#### Render

```
/model render <模型名> [size] [rotx] [roty] [rotz]
```

使用text_display方式渲染模型，后续提供可选参数调整模型的大小和方向，模型生成位置为玩家位置，该指令只能由玩家（op）执行，可以重复渲染相同模型。

#### Remove

```
/model remove <模型名>
```

删除所有同一名称模型。

#### Block

```
/model block render <模型名> [size] [rotx] [roty] [rotz]
```

使用block_display方式渲染模型，需要配合worldedit原理图schem使用，将schem模型放置到插件文件夹中schem文件夹下使用.

[TODO]\:

```
/model block rotate <模型名> [speed]
```

让模型旋转起来~



剩余内容详见[英文文档](./README_EN.md)