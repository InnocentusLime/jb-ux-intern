package com.github.innocentuslime.jbuxintern.toolWindow

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.psi.*
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.containers.stream
import javax.swing.tree.DefaultMutableTreeNode


class MyToolWindowFactory : ToolWindowFactory {

    init {
        thisLogger().warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")
    }

    private val contentFactory = ContentFactory.SERVICE.getInstance()

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myToolWindow = MyToolWindow(toolWindow)
        val content = contentFactory.createContent(myToolWindow.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class MyToolWindow(toolWindow: ToolWindow) {

        private val psiManager = PsiManager.getInstance(toolWindow.project)
        private val projectScope = GlobalSearchScope.projectScope(toolWindow.project)

        private fun countClassesAndFunctions(cl: PsiClass): Pair<Int, Int> {
            var classCounter = 0
            var funcCounter = 0
            cl.accept(object : JavaRecursiveElementVisitor() {
                override fun visitClass(aClass: PsiClass?) {
                    classCounter += 1
                    if (aClass == null) {
                        return
                    }

                    aClass.innerClasses.forEach { x -> x.accept(this) }
                    aClass.methods.forEach { x -> x.accept(this) }
                }

                override fun visitMethod(method: PsiMethod?) {
                    funcCounter += 1
                    if (method == null) {
                        return
                    }

                    method.body?.accept(this)
                }
            })

            return Pair(classCounter, funcCounter)
        }

        private fun classesAndMethodsInFile(file: PsiJavaFile): Pair<Int, Int> {
            return file.classes.stream().map { x -> countClassesAndFunctions(x) }
                .reduce(Pair(0, 0)) {
                    (x1, y1), (x2, y2) -> Pair(x1 + x2, y1 + y2)
                }
        }

        fun getContent() = JBPanel<JBPanel<*>>().apply {
            val root = DefaultMutableTreeNode("Class and method count")
            val tree = Tree(root)

            FileTypeIndex.getFiles(
                JavaFileType.INSTANCE,
                projectScope,
            ).stream()
                .map(psiManager::findFile)
                .filter { it is PsiJavaFile }
                .map { it as PsiJavaFile }
                .forEach { file ->
                    val fileNode = DefaultMutableTreeNode(file.name)
                    val (classCount, methodCount) = classesAndMethodsInFile(file)
                    val classNode = DefaultMutableTreeNode("Class count: $classCount")
                    val methodNode = DefaultMutableTreeNode("Method count: $methodCount")

                    fileNode.add(classNode)
                    fileNode.add(methodNode)
                    root.add(fileNode)
                }

            add(tree)
        }
    }
}
