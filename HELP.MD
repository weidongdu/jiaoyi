要将子模块作为普通目录添加到Git仓库中，可以先从仓库中移除子模块，然后将其作为普通文件添加到仓库中。请遵循以下步骤操作：

1. 删除子模块：

   在主仓库中，使用以下命令删除子模块：
   ````
   git submodule deinit <submodule_path>
   git rm --cached <submodule_path>
   ```
   这里，`<submodule_path>` 是子模块在主仓库中的相对路径。

2. 清除`.gitmodules`文件：

   使用文本编辑器打开`.gitmodules`文件，找到与子模块相关的部分，并将其删除。然后保存并关闭文件。

3. 删除子模块的Git目录：

   在子模块的目录中，删除`.git`文件夹。在Linux或macOS系统中，可以使用以下命令：
   ````
   rm -rf <submodule_path>/.git
   ```
   或者在Windows系统中，使用：
   ````
   rmdir /s /q <submodule_path>\.git
   ```

4. 将子模块作为普通文件添加到主仓库：

   现在，子模块已经变成了普通目录。在主仓库中，使用以下命令将子模块添加到版本控制中：
   ````
   git add <submodule_path>
   git commit -m "Add submodule as a regular directory"
   git push
   ```
   这样，子模块就不再作为Git子模块，而是作为普通文件夹添加到了主仓库。