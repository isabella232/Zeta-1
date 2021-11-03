import MetadataSampleQuery from "./sample-query.vue";
export const RICH_TEXT_EDITOR_OPTION =  {
	modules: {
		toolbar:[
			['bold', 'italic', 'underline', 'strike'],
			[{ 'list': 'ordered' }, { 'list': 'bullet' }],
			[{ 'direction': 'rtl' }],
			[{ 'size': ['small', false, 'large', 'huge'] }],
			[{ 'font': [] }],
			[{ 'color': [] }, { 'background': [] }],
			['clean'],
			['link', 'image']
		]
	}
}
export default MetadataSampleQuery;