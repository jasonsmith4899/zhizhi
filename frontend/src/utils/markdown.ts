import { marked } from 'marked'
import DOMPurify from 'dompurify'

// 配置 marked：不使用 async，输出 HTML
marked.setOptions({
  breaks: true,      // 换行符转 <br>
  gfm: true,         // GitHub 风格 markdown
})

/**
 * 预处理文本，修复常见的 markdown 格式问题
 * AI 常返回 "-内容" 而非 "- 内容"，导致列表不被识别
 */
function preprocessMarkdown(text: string): string {
  // 修复列表标记：行首 "-内容" → "- 内容"，"*内容" → "* 内容"
  // 只处理紧跟非空格字符的情况，避免影响 "---" 分割线
  return text.replace(/^([ \t]*)([-*])\s*(?=\S)/gm, (indent, marker) => {
    return `${indent}${marker} `
  })
}

/**
 * 将 markdown 文本转为安全的 HTML
 */
export function renderMarkdown(text: string): string {
  if (!text) return ''
  const preprocessed = preprocessMarkdown(text)
  const rawHtml = marked.parse(preprocessed) as string
  return DOMPurify.sanitize(rawHtml, {
    ADD_TAGS: ['iframe'],
    ADD_ATTR: ['target'],
  })
}

/**
 * 从 AI 回复中提取 "信息来源：..." 文本，并返回：
 * - cleanContent: 去掉来源行后的正文
 * - inlineSources: 解析出的来源列表 [{ name, raw }]
 */
export function extractInlineSources(text: string): {
  cleanContent: string
  inlineSources: { name: string; raw: string }[]
} {
  if (!text) return { cleanContent: text, inlineSources: [] }

  const sources: { name: string; raw: string }[] = []
  // 匹配行首的来源标记：行首可能有空白，然后是"信息来源"/"来源"/"参考"
  // 只匹配行首，避免误匹配正文中提到的"来源"二字
  const sourcePattern = /^[ \t]*(?:信息来源|参考来源|来源)[：:]\s*(.+?)$/gm
  let cleanContent = text

  let match
  while ((match = sourcePattern.exec(text)) !== null) {
    const rawLine = match[1].trim()
    // 按逗号、顿号、分号拆分多个来源
    const parts = rawLine.split(/[,，、；;]+/).map(s => s.trim()).filter(Boolean)
    for (const part of parts) {
      // 去掉页码引用如 "2.1处理流程" 和括号内容如 "（51%）"
      const clean = part
        .replace(/^\d+(\.\d+)*\s*/, '')
        .replace(/[（(][^）)]*[）)]/g, '')
        .trim()
      if (clean) {
        sources.push({ name: clean, raw: part })
      }
    }
    // 从正文中移除来源行
    cleanContent = cleanContent.replace(match[0], '')
  }

  // 清理多余空行
  cleanContent = cleanContent.replace(/\n{3,}/g, '\n\n').trim()

  return { cleanContent, inlineSources: sources }
}
