package com.github.innocentuslime.jbuxintern.toolWindow

import com.github.innocentuslime.jbuxintern.MyBundle
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import javax.swing.JButton


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

        private val editorManager = FileEditorManager.getInstance(toolWindow.project)
        private val docManager = FileDocumentManager.getInstance()
        private val psiManager = PsiManager.getInstance(toolWindow.project)

        private fun getPsiFile(): PsiFile? {
            val currentDoc = editorManager.selectedTextEditor?.document
            val currentFile = if (currentDoc != null) docManager.getFile(currentDoc) else null

            currentFile?.let { return psiManager.findFile(it) }

            return null
        }

        fun getContent() = JBPanel<JBPanel<*>>().apply {
            val label = JBLabel(MyBundle.message("classCount", "?"))

            add(label)
            add(JButton(MyBundle.message("countClasses")).apply {
                addActionListener {
                    var counter = 0
                    getPsiFile()?.accept(object : JavaRecursiveElementVisitor() {
                        override fun visitClass(aClass: PsiClass?) {
                            counter += 1
                            if (aClass == null) {
                                return
                            }

                            aClass.allInnerClasses.forEach { x -> x.accept(this) }
                            aClass.allMethods.forEach { x -> x.accept(this) }
                        }
                    })
                    label.text = MyBundle.message("classCount", counter)
                }
            })
        }
    }
}
