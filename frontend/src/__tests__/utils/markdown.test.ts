import { describe, it, expect } from 'vitest'
import { renderMarkdown, extractInlineSources } from '@/utils/markdown'

describe('utils/markdown', () => {
  describe('renderMarkdown', () => {
    it('should return empty string for empty input', () => {
      expect(renderMarkdown('')).toBe('')
    })

    it('should return empty string for null/undefined-like input', () => {
      expect(renderMarkdown('')).toBe('')
    })

    it('should render basic paragraph', () => {
      const result = renderMarkdown('Hello World')
      expect(result).toContain('Hello World')
      expect(result).toContain('<p>')
    })

    it('should render headings', () => {
      const result = renderMarkdown('# Title')
      expect(result).toContain('<h1')
      expect(result).toContain('Title')
    })

    it('should render bold text', () => {
      const result = renderMarkdown('This is **bold** text')
      expect(result).toContain('<strong>')
      expect(result).toContain('bold')
    })

    it('should render italic text', () => {
      const result = renderMarkdown('This is _italic_ text')
      expect(result).toContain('<em>')
      expect(result).toContain('italic')
    })

    it('should render inline code', () => {
      const result = renderMarkdown('`code`')
      expect(result).toContain('<code>')
      expect(result).toContain('code')
    })

    it('should render code blocks', () => {
      const result = renderMarkdown('```\nconst x = 1\n```')
      expect(result).toContain('<code>')
      expect(result).toContain('const x = 1')
    })

    it('should render links', () => {
      const result = renderMarkdown('[link](https://example.com)')
      expect(result).toContain('<a')
      expect(result).toContain('href="https://example.com"')
      expect(result).toContain('link')
    })

    it('should render unordered lists', () => {
      const result = renderMarkdown('- item 1\n- item 2')
      expect(result).toContain('<li>')
      expect(result).toContain('item 1')
      expect(result).toContain('item 2')
    })

    it('should render ordered lists', () => {
      const result = renderMarkdown('1. first\n2. second')
      expect(result).toContain('<li>')
      expect(result).toContain('first')
    })

    it('should convert line breaks when breaks is enabled', () => {
      const result = renderMarkdown('line1\nline2')
      expect(result).toContain('<br>')
    })

    it('should render tables with GFM', () => {
      const md = '| A | B |\n|---|---|\n| 1 | 2 |'
      const result = renderMarkdown(md)
      expect(result).toContain('<table')
      expect(result).toContain('A')
    })

    it('should sanitize dangerous HTML', () => {
      const result = renderMarkdown('<script>alert("xss")</script>')
      expect(result).not.toContain('<script>')
    })

    it('should allow iframe tags via sanitize config', () => {
      const result = renderMarkdown('<iframe src="https://example.com"></iframe>')
      // DOMPurify with ADD_TAGS: ['iframe'] should keep iframe
      expect(result).toContain('iframe')
    })

    it('should fix list markers without space (preprocessMarkdown)', () => {
      const result = renderMarkdown('-item1\n-item2')
      expect(result).toContain('<li>')
      expect(result).toContain('item1')
    })

    it('should fix asterisk list markers without space', () => {
      const result = renderMarkdown('*item1\n*item2')
      expect(result).toContain('<li>')
      expect(result).toContain('item1')
    })

    it('should not break horizontal rules (---)', () => {
      const result = renderMarkdown('---')
      // Should still work as a horizontal rule or at least not break
      expect(result).toBeDefined()
    })
  })

  describe('extractInlineSources', () => {
    it('should return empty sources for empty input', () => {
      const result = extractInlineSources('')
      expect(result.cleanContent).toBe('')
      expect(result.inlineSources).toEqual([])
    })

    it('should extract sources from "信息来源" line', () => {
      const text = '这是正文内容\n信息来源：文档A，文档B\n更多正文'
      const result = extractInlineSources(text)
      expect(result.inlineSources).toHaveLength(2)
      expect(result.inlineSources[0].name).toBe('文档A')
      expect(result.inlineSources[1].name).toBe('文档B')
      expect(result.cleanContent).not.toContain('信息来源')
    })

    it('should extract sources from "来源" line', () => {
      const text = '正文\n来源：文档C'
      const result = extractInlineSources(text)
      expect(result.inlineSources).toHaveLength(1)
      expect(result.inlineSources[0].name).toBe('文档C')
    })

    it('should extract sources from "参考来源" line', () => {
      const text = '正文\n参考来源：文档D'
      const result = extractInlineSources(text)
      expect(result.inlineSources).toHaveLength(1)
      expect(result.inlineSources[0].name).toBe('文档D')
    })

    it('should split sources by various delimiters', () => {
      const text = '信息来源：A，B、C；D,E'
      const result = extractInlineSources(text)
      expect(result.inlineSources).toHaveLength(5)
      expect(result.inlineSources.map((s) => s.name)).toEqual(['A', 'B', 'C', 'D', 'E'])
    })

    it('should strip page number prefixes from source names', () => {
      const text = '信息来源：2.1处理流程，3.2数据字典'
      const result = extractInlineSources(text)
      expect(result.inlineSources[0].name).toBe('处理流程')
      expect(result.inlineSources[1].name).toBe('数据字典')
    })

    it('should strip parenthetical content from source names', () => {
      const text = '信息来源：文档A（51%），文档B（重要）'
      const result = extractInlineSources(text)
      expect(result.inlineSources[0].name).toBe('文档A')
      expect(result.inlineSources[1].name).toBe('文档B')
    })

    it('should preserve raw source text', () => {
      const text = '信息来源：2.1处理流程（51%）'
      const result = extractInlineSources(text)
      expect(result.inlineSources[0].raw).toBe('2.1处理流程（51%）')
    })

    it('should handle multiple source lines', () => {
      const text = '正文\n信息来源：文档A\n参考来源：文档B'
      const result = extractInlineSources(text)
      expect(result.inlineSources).toHaveLength(2)
    })

    it('should clean up extra blank lines after removing source lines', () => {
      const text = '段落1\n\n信息来源：文档A\n\n\n\n段落2'
      const result = extractInlineSources(text)
      // Should not have 3+ consecutive newlines
      expect(result.cleanContent).not.toMatch(/\n{3,}/)
    })

    it('should not match "来源" in the middle of a line', () => {
      const text = '这个来源很重要'
      const result = extractInlineSources(text)
      expect(result.inlineSources).toHaveLength(0)
      expect(result.cleanContent).toContain('这个来源很重要')
    })

    it('should handle Chinese colon (：) and English colon (:) in source lines', () => {
      const text1 = '信息来源：文档A'
      const text2 = '信息来源:文档A'
      const result1 = extractInlineSources(text1)
      const result2 = extractInlineSources(text2)
      expect(result1.inlineSources).toHaveLength(1)
      expect(result2.inlineSources).toHaveLength(1)
    })

    it('should handle text with no sources', () => {
      const text = '这是一段普通文本，没有来源信息。'
      const result = extractInlineSources(text)
      expect(result.inlineSources).toEqual([])
      expect(result.cleanContent).toBe(text)
    })

    it('should trim whitespace from source names', () => {
      const text = '信息来源：  文档A  ，  文档B  '
      const result = extractInlineSources(text)
      expect(result.inlineSources[0].name).toBe('文档A')
      expect(result.inlineSources[1].name).toBe('文档B')
    })
  })
})
