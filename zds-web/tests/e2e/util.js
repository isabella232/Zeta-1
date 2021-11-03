const $ = require('jquery')
const Util = {
    getCodeMirrorValue($el){
        let textArr = [];
        
        const $lines = $el.find('.CodeMirror-code .CodeMirror-line')
        $lines.each(i => {
            const line = $lines[i]
            textArr.push($(line).text().trim())
        })
        return textArr.join('');
    }
}
module.exports = Util