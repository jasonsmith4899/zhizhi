import { config } from '@vue/test-utils'
import ElementPlus from 'element-plus'

// Register Element Plus globally for all tests
config.global.plugins = [ElementPlus]
