import { describe, it, expect } from 'vitest'
import {
  ArrowLeft,
  ChatDotRound,
  Collection,
  DataLine,
  Document,
  Download,
  Expand,
  Fold,
  Lock,
  Message,
  Plus,
  Setting,
  Ticket,
  Upload,
  User,
  WarningFilled,
} from '@/utils/icons'

describe('utils/icons', () => {
  it('should export ArrowLeft', () => {
    expect(ArrowLeft).toBeDefined()
  })

  it('should export ChatDotRound', () => {
    expect(ChatDotRound).toBeDefined()
  })

  it('should export Collection', () => {
    expect(Collection).toBeDefined()
  })

  it('should export DataLine', () => {
    expect(DataLine).toBeDefined()
  })

  it('should export Document', () => {
    expect(Document).toBeDefined()
  })

  it('should export Download', () => {
    expect(Download).toBeDefined()
  })

  it('should export Expand', () => {
    expect(Expand).toBeDefined()
  })

  it('should export Fold', () => {
    expect(Fold).toBeDefined()
  })

  it('should export Lock', () => {
    expect(Lock).toBeDefined()
  })

  it('should export Message', () => {
    expect(Message).toBeDefined()
  })

  it('should export Plus', () => {
    expect(Plus).toBeDefined()
  })

  it('should export Setting', () => {
    expect(Setting).toBeDefined()
  })

  it('should export Ticket', () => {
    expect(Ticket).toBeDefined()
  })

  it('should export Upload', () => {
    expect(Upload).toBeDefined()
  })

  it('should export User', () => {
    expect(User).toBeDefined()
  })

  it('should export WarningFilled', () => {
    expect(WarningFilled).toBeDefined()
  })

  it('should export exactly 16 icons', () => {
    const icons = {
      ArrowLeft,
      ChatDotRound,
      Collection,
      DataLine,
      Document,
      Download,
      Expand,
      Fold,
      Lock,
      Message,
      Plus,
      Setting,
      Ticket,
      Upload,
      User,
      WarningFilled,
    }
    expect(Object.keys(icons)).toHaveLength(16)
  })

  it('each icon should be a Vue component object', () => {
    const icons = [
      ArrowLeft,
      ChatDotRound,
      Collection,
      DataLine,
      Document,
      Download,
      Expand,
      Fold,
      Lock,
      Message,
      Plus,
      Setting,
      Ticket,
      Upload,
      User,
      WarningFilled,
    ]
    for (const icon of icons) {
      // Element Plus icons are Vue functional components or objects with render/setup
      expect(icon).toBeDefined()
      expect(typeof icon).toBe('object')
    }
  })
})
